package io.github.baikoff.whattocook.assistant.service;

import io.github.baikoff.whattocook.assistant.service.model.AssistantCriteria;
import io.github.baikoff.whattocook.assistant.service.model.AssistantGoal;
import io.github.baikoff.whattocook.assistant.service.model.DishRecommendation;
import io.github.baikoff.whattocook.favorite.service.RecipeFavoriteService;
import io.github.baikoff.whattocook.history.service.CookingHistoryService;
import io.github.baikoff.whattocook.history.service.model.CookingHistoryItem;
import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.domain.CookingMethod;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.Equipment;
import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.repository.CookingMethodRepository;
import io.github.baikoff.whattocook.recipe.repository.EquipmentRepository;
import io.github.baikoff.whattocook.recipe.repository.IngredientRepository;
import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionEvaluation;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionService;
import io.github.baikoff.whattocook.restriction.service.RestrictionPolicy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishRecommendationService {

	private static final int RECOMMENDATION_LIMIT = 3;
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private final RecipeRepository recipeRepository;
	private final IngredientRepository ingredientRepository;
	private final EquipmentRepository equipmentRepository;
	private final CookingMethodRepository cookingMethodRepository;
	private final RecipeRestrictionService restrictionService;
	private final CookingHistoryService cookingHistoryService;
	private final RecipeFavoriteService favoriteService;
	private final RecipeImageStorage imageStorage;

	public DishRecommendationService(
			RecipeRepository recipeRepository,
			IngredientRepository ingredientRepository,
			EquipmentRepository equipmentRepository,
			CookingMethodRepository cookingMethodRepository,
			RecipeRestrictionService restrictionService,
			CookingHistoryService cookingHistoryService,
			RecipeFavoriteService favoriteService,
			RecipeImageStorage imageStorage) {
		this.recipeRepository = recipeRepository;
		this.ingredientRepository = ingredientRepository;
		this.equipmentRepository = equipmentRepository;
		this.cookingMethodRepository = cookingMethodRepository;
		this.restrictionService = restrictionService;
		this.cookingHistoryService = cookingHistoryService;
		this.favoriteService = favoriteService;
		this.imageStorage = imageStorage;
	}

	@Transactional(readOnly = true)
	public List<SelectionOption> getIngredientOptions() {
		return ingredientRepository.findAllByOrderByNameRuAsc().stream()
				.map(ingredient -> new SelectionOption(ingredient.getId(), ingredient.getNameRu()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SelectionOption> getEquipmentOptions() {
		return equipmentRepository.findAllByOrderByNameRuAsc().stream()
				.map(equipment -> new SelectionOption(equipment.getId(), equipment.getNameRu()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SelectionOption> getCookingMethodOptions() {
		return cookingMethodRepository.findAllByOrderByNameRuAsc().stream()
				.map(method -> new SelectionOption(method.getId(), method.getNameRu()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DishRecommendation> recommend(AssistantCriteria requestedCriteria) {
		var criteria = requestedCriteria.surprise()
				? AssistantCriteria.surpriseMe()
				: requestedCriteria;
		var selectedIngredientIds = normalizeIngredientIds(criteria.ingredientIds());
		var cookingStats = cookingStats();
		var favoriteRecipeIds = favoriteService.getFavoriteRecipeIds();
		var restrictionPolicy = restrictionService.loadPolicy();

		return recipeRepository.findAllByStatusNotOrderByNameRuAsc(RecipeStatus.ARCHIVED).stream()
				.filter(recipe -> matchesFilters(recipe, criteria))
				.map(recipe -> evaluate(recipe, restrictionPolicy))
				.filter(evaluated -> !evaluated.restriction().excluded())
				.map(evaluated -> score(
						evaluated.recipe(),
						evaluated.restriction(),
						criteria,
						selectedIngredientIds,
						cookingStats.getOrDefault(evaluated.recipe().getId(), CookingStats.empty()),
						favoriteRecipeIds.contains(evaluated.recipe().getId())))
				.filter(scored -> selectedIngredientIds.isEmpty()
						|| scored.recommendation().matchedIngredientCount() > 0)
				.sorted(recommendationOrder(criteria.goal()))
				.limit(RECOMMENDATION_LIMIT)
				.map(ScoredRecommendation::recommendation)
				.toList();
	}

	private Set<Long> normalizeIngredientIds(List<Long> requestedIds) {
		if (requestedIds.isEmpty()) {
			return Set.of();
		}
		var knownIds = ingredientRepository.findAllById(requestedIds).stream()
				.map(Ingredient::getId)
				.collect(Collectors.toSet());
		return Set.copyOf(knownIds);
	}

	private Map<Long, CookingStats> cookingStats() {
		var mutableStats = new HashMap<Long, MutableCookingStats>();
		for (CookingHistoryItem item : cookingHistoryService.getHistory()) {
			var stats = mutableStats.computeIfAbsent(item.recipeId(), ignored -> new MutableCookingStats());
			stats.record(item);
		}
		return mutableStats.entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().snapshot()));
	}

	private boolean matchesFilters(Recipe recipe, AssistantCriteria criteria) {
		if (criteria.maxMinutes() != null && totalMinutes(recipe) > criteria.maxMinutes()) {
			return false;
		}
		if (criteria.equipmentId() != null && recipe.getEquipment().stream()
				.noneMatch(equipment -> equipment.getId().equals(criteria.equipmentId()))) {
			return false;
		}
		if (criteria.cookingMethodId() != null && recipe.getCookingMethods().stream()
				.noneMatch(method -> method.getId().equals(criteria.cookingMethodId()))) {
			return false;
		}
		return criteria.maxDifficulty() == null
				|| difficultyRank(recipe.getDifficulty()) <= difficultyRank(criteria.maxDifficulty());
	}

	private ScoredRecommendation score(
			Recipe recipe,
			RecipeRestrictionEvaluation restriction,
			AssistantCriteria criteria,
			Set<Long> selectedIngredientIds,
			CookingStats cookingStats,
			boolean favorite) {
		var requiredIngredients = recipe.getIngredients().stream()
				.filter(ingredient -> !ingredient.isOptional())
				.toList();
		var ingredientsSpecified = !selectedIngredientIds.isEmpty();
		var missingIngredients = ingredientsSpecified
				? requiredIngredients.stream()
						.filter(ingredient -> !selectedIngredientIds.contains(ingredient.getIngredient().getId()))
						.map(ingredient -> ingredient.getIngredient().getNameRu())
						.toList()
				: List.<String>of();
		var requiredCount = requiredIngredients.size();
		var matchedCount = ingredientsSpecified ? requiredCount - missingIngredients.size() : 0;
		var matchPercentage = ingredientsSpecified && requiredCount > 0
				? (int) Math.round(matchedCount * 100.0 / requiredCount)
				: 0;

		var recommendation = new DishRecommendation(
				recipe.getId(),
				recipe.getNameRu(),
				recipe.getDescription(),
				imageStorage.publicUrl(recipe.getMainImagePath()),
				totalMinutes(recipe),
				recipe.getDifficulty().getDisplayName(),
				recipe.getEquipment().stream().map(Equipment::getNameRu).toList(),
				recipe.getCookingMethods().stream().map(CookingMethod::getNameRu).toList(),
				ingredientsSpecified,
				matchPercentage,
				matchedCount,
				requiredCount,
				missingIngredients,
				cookingStats.lastCookedOn(),
				favorite,
				cookingStats.averageRating(),
				reasons(
						recipe,
						criteria,
						ingredientsSpecified,
						matchedCount,
						requiredCount,
						missingIngredients,
						cookingStats,
						favorite),
				restriction.warnings());
		return new ScoredRecommendation(
				recommendation,
				matchPercentage,
				cookingStats.lastCookedOn(),
				favorite,
				cookingStats.averageRating());
	}

	private List<String> reasons(
			Recipe recipe,
			AssistantCriteria criteria,
			boolean ingredientsSpecified,
			int matchedCount,
			int requiredCount,
			List<String> missingIngredients,
			CookingStats cookingStats,
			boolean favorite) {
		var reasons = new ArrayList<String>();
		if (ingredientsSpecified) {
			reasons.add(missingIngredients.isEmpty()
					? "Все обязательные продукты уже есть"
					: "Есть %d из %d обязательных продуктов".formatted(matchedCount, requiredCount));
		}
		if (criteria.maxMinutes() != null) {
			reasons.add("Укладывается в лимит %d мин".formatted(criteria.maxMinutes()));
		}
		if (criteria.equipmentId() != null) {
			recipe.getEquipment().stream()
					.filter(equipment -> equipment.getId().equals(criteria.equipmentId()))
					.findFirst()
					.ifPresent(equipment -> reasons.add("Подходит для оборудования: " + equipment.getNameRu()));
		}
		if (criteria.cookingMethodId() != null) {
			recipe.getCookingMethods().stream()
					.filter(method -> method.getId().equals(criteria.cookingMethodId()))
					.findFirst()
					.ifPresent(method -> reasons.add("Способ приготовления: " + method.getNameRu()));
		}
		if (criteria.maxDifficulty() != null) {
			reasons.add("Сложность: " + recipe.getDifficulty().getDisplayName());
		}
		if (cookingStats.lastCookedOn() == null) {
			reasons.add("Это блюдо ещё не отмечалось приготовленным");
		} else {
			reasons.add("Последний раз готовилось " + DATE_FORMAT.format(cookingStats.lastCookedOn()));
		}
		addGoalReasons(reasons, recipe, criteria.goal(), favorite, cookingStats);
		if (criteria.surprise()) {
			reasons.add("Предложено для разнообразия меню");
		}
		return List.copyOf(reasons);
	}

	private void addGoalReasons(
			List<String> reasons,
			Recipe recipe,
			AssistantGoal goal,
			boolean favorite,
			CookingStats cookingStats) {
		switch (goal) {
			case BALANCED -> reasons.add("Учтены продукты, время и история приготовления");
			case USE_PRODUCTS -> reasons.add("Приоритет отдан совпадению имеющихся продуктов");
			case QUICK -> reasons.add("Общее время приготовления — %d мин".formatted(totalMinutes(recipe)));
			case NEW -> reasons.add(cookingStats.lastCookedOn() == null
					? "Подходит, чтобы попробовать новое"
					: "Выбрано среди блюд, которые готовились давно");
			case FAVORITE -> {
				if (favorite) {
					reasons.add("Рецепт добавлен в избранное");
				}
				if (cookingStats.averageRating() != null) {
					reasons.add(String.format(
							Locale.forLanguageTag("ru-RU"),
							"Средняя личная оценка — %.1f из 5",
							cookingStats.averageRating()));
				}
			}
		}
	}

	private Comparator<ScoredRecommendation> recommendationOrder(AssistantGoal goal) {
		Comparator<ScoredRecommendation> byMatch = Comparator
				.comparingInt(ScoredRecommendation::matchPercentage)
				.reversed();
		Comparator<ScoredRecommendation> byNovelty = Comparator
				.comparing((ScoredRecommendation item) -> item.lastCookedOn() != null)
				.thenComparing(
						ScoredRecommendation::lastCookedOn,
						Comparator.nullsFirst(Comparator.naturalOrder()))
				.thenComparing(byMatch);
		Comparator<ScoredRecommendation> byTime = Comparator
				.comparingInt((ScoredRecommendation item) -> item.recommendation().totalMinutes())
				.thenComparing(byMatch);
		Comparator<ScoredRecommendation> byFavorite = Comparator
				.comparing(ScoredRecommendation::favorite)
				.reversed()
				.thenComparing(
						ScoredRecommendation::averageRating,
						Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(byMatch);

		var goalOrder = switch (goal) {
			case BALANCED, USE_PRODUCTS -> byMatch.thenComparing(byNovelty).thenComparing(byTime);
			case QUICK -> byTime.thenComparing(byNovelty);
			case NEW -> byNovelty.thenComparing(byTime);
			case FAVORITE -> byFavorite.thenComparing(byNovelty).thenComparing(byTime);
		};
		return goalOrder.thenComparing(
				item -> item.recommendation().nameRu(),
				String.CASE_INSENSITIVE_ORDER);
	}

	private int totalMinutes(Recipe recipe) {
		return recipe.getPreparationMinutes() + recipe.getCookingMinutes();
	}

	private int difficultyRank(Difficulty difficulty) {
		return switch (difficulty) {
			case EASY -> 1;
			case MEDIUM -> 2;
			case HARD -> 3;
		};
	}

	private EvaluatedRecipe evaluate(Recipe recipe, RestrictionPolicy policy) {
		return new EvaluatedRecipe(recipe, restrictionService.evaluate(recipe, policy));
	}

	private record EvaluatedRecipe(Recipe recipe, RecipeRestrictionEvaluation restriction) {
	}

	private record ScoredRecommendation(
			DishRecommendation recommendation,
			int matchPercentage,
			LocalDate lastCookedOn,
			boolean favorite,
			Double averageRating) {
	}

	private record CookingStats(LocalDate lastCookedOn, Double averageRating) {

		private static CookingStats empty() {
			return new CookingStats(null, null);
		}
	}

	private static class MutableCookingStats {

		private LocalDate lastCookedOn;
		private int ratingSum;
		private int ratingCount;

		private void record(CookingHistoryItem item) {
			if (lastCookedOn == null || item.cookedOn().isAfter(lastCookedOn)) {
				lastCookedOn = item.cookedOn();
			}
			if (item.rating() != null) {
				ratingSum += item.rating();
				ratingCount++;
			}
		}

		private CookingStats snapshot() {
			return new CookingStats(
					lastCookedOn,
					ratingCount == 0 ? null : (double) ratingSum / ratingCount);
		}
	}
}
