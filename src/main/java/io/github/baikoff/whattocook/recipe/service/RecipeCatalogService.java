package io.github.baikoff.whattocook.recipe.service;

import io.github.baikoff.whattocook.favorite.repository.RecipeFavoriteRepository;
import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.domain.CookingMethod;
import io.github.baikoff.whattocook.recipe.domain.Equipment;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeIngredient;
import io.github.baikoff.whattocook.recipe.domain.RecipeSearchCriteria;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.recipe.repository.RecipeSpecifications;
import io.github.baikoff.whattocook.recipe.service.model.RecipeCatalogItem;
import io.github.baikoff.whattocook.recipe.service.model.RecipeDetails;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionEvaluation;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionService;
import io.github.baikoff.whattocook.restriction.service.RestrictionPolicy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeCatalogService {

	private final RecipeRepository recipeRepository;
	private final RecipeFavoriteRepository favoriteRepository;
	private final RecipeRestrictionService restrictionService;
	private final RecipeImageStorage imageStorage;
	private final RecipePortionCalculator portionCalculator;

	public RecipeCatalogService(
			RecipeRepository recipeRepository,
			RecipeFavoriteRepository favoriteRepository,
			RecipeRestrictionService restrictionService,
			RecipeImageStorage imageStorage,
			RecipePortionCalculator portionCalculator) {
		this.recipeRepository = recipeRepository;
		this.favoriteRepository = favoriteRepository;
		this.restrictionService = restrictionService;
		this.imageStorage = imageStorage;
		this.portionCalculator = portionCalculator;
	}

	@Transactional(readOnly = true)
	public List<RecipeCatalogItem> getArchive() {
		var favoriteIds = getFavoriteIds();
		return recipeRepository.findAllByStatusOrderByNameRuAsc(RecipeStatus.ARCHIVED)
				.stream()
				.map(recipe -> toCatalogItem(recipe, favoriteIds, List.of()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RecipeCatalogItem> getFavorites() {
		var favoriteIds = favoriteRepository.findAllRecipeIds();
		if (favoriteIds.isEmpty()) {
			return List.of();
		}

		var policy = restrictionService.loadPolicy();
		return recipeRepository.findAllByIdInAndStatusNotOrderByNameRuAsc(favoriteIds, RecipeStatus.ARCHIVED)
				.stream()
				.map(recipe -> evaluate(recipe, policy))
				.filter(evaluated -> !evaluated.restriction().excluded())
				.map(evaluated -> toCatalogItem(
						evaluated.recipe(),
						Set.copyOf(favoriteIds),
						evaluated.restriction().warnings()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RecipeCatalogItem> getCatalog() {
		return search(RecipeSearchCriteria.empty());
	}

	@Transactional(readOnly = true)
	public List<RecipeCatalogItem> search(RecipeSearchCriteria criteria) {
		var favoriteIds = getFavoriteIds();
		var policy = restrictionService.loadPolicy();
		return recipeRepository.findAll(
				RecipeSpecifications.matches(criteria),
				Sort.by(Sort.Direction.ASC, "nameRu"))
				.stream()
				.map(recipe -> evaluate(recipe, policy))
				.filter(evaluated -> !evaluated.restriction().excluded())
				.map(evaluated -> toCatalogItem(
						evaluated.recipe(),
						favoriteIds,
						evaluated.restriction().warnings()))
				.toList();
	}

	@Transactional(readOnly = true)
	public RecipeDetails getRecipe(long id) {
		return getRecipe(id, null);
	}

	@Transactional(readOnly = true)
	public RecipeDetails getRecipe(long id, Integer requestedServings) {
		var recipe = recipeRepository.findByIdAndStatusNot(id, RecipeStatus.ARCHIVED)
				.orElseThrow(() -> new RecipeNotFoundException(id));

		var restriction = restrictionService.evaluate(recipe, restrictionService.loadPolicy());
		return toDetails(recipe, requestedServings, restriction);
	}

	private RecipeCatalogItem toCatalogItem(
			Recipe recipe,
			Set<Long> favoriteIds,
			List<RestrictionWarning> restrictionWarnings) {
		return new RecipeCatalogItem(
				recipe.getId(),
				recipe.getNameRu(),
				recipe.getNameEn(),
				recipe.getDescription(),
				imageStorage.publicUrl(recipe.getMainImagePath()),
				totalMinutes(recipe),
				recipe.getServings(),
				recipe.getDifficulty().getDisplayName(),
				recipe.getStatus().getDisplayName(),
				favoriteIds.contains(recipe.getId()),
				restrictionWarnings);
	}

	private RecipeDetails toDetails(
			Recipe recipe,
			Integer requestedServings,
			RecipeRestrictionEvaluation restriction) {
		var servings = portionCalculator.resolveServings(requestedServings, recipe.getServings());
		var ingredients = recipe.getIngredients()
				.stream()
				.map(recipeIngredient -> new RecipeDetails.IngredientLine(
						recipeIngredient.getIngredient().getNameRu(),
						formatAmount(portionCalculator.recalculate(
								recipeIngredient.getAmount(),
								recipe.getServings(),
								servings)),
						recipeIngredient.getUnit().getDisplayName(),
						recipeIngredient.isOptional()))
				.toList();

		var steps = recipe.getSteps()
				.stream()
				.map(step -> new RecipeDetails.Step(
						step.getSortOrder(),
						step.getInstruction(),
						imageStorage.publicUrl(step.getImagePath())))
				.toList();

		var equipment = recipe.getEquipment()
				.stream()
				.map(Equipment::getNameRu)
				.toList();

		var cookingMethods = recipe.getCookingMethods()
				.stream()
				.map(CookingMethod::getNameRu)
				.toList();

		return new RecipeDetails(
				recipe.getId(),
				recipe.getNameRu(),
				recipe.getNameEn(),
				recipe.getDescription(),
				imageStorage.publicUrl(recipe.getMainImagePath()),
				recipe.getServings(),
				servings,
				recipe.getPreparationMinutes(),
				recipe.getCookingMinutes(),
				totalMinutes(recipe),
				recipe.getDifficulty().getDisplayName(),
				recipe.getStatus().getDisplayName(),
				recipe.getSourceName(),
				recipe.getSourceUrl(),
				recipe.getPersonalNotes(),
				ingredients,
				steps,
				equipment,
				cookingMethods,
				favoriteRepository.existsById(recipe.getId()),
				restriction.excluded(),
				restriction.warnings());
	}

	private EvaluatedRecipe evaluate(Recipe recipe, RestrictionPolicy policy) {
		return new EvaluatedRecipe(recipe, restrictionService.evaluate(recipe, policy));
	}

	private Set<Long> getFavoriteIds() {
		return Set.copyOf(favoriteRepository.findAllRecipeIds());
	}

	private int totalMinutes(Recipe recipe) {
		return recipe.getPreparationMinutes() + recipe.getCookingMinutes();
	}

	private String formatAmount(BigDecimal amount) {
		return amount.stripTrailingZeros().toPlainString();
	}

	private record EvaluatedRecipe(Recipe recipe, RecipeRestrictionEvaluation restriction) {
	}
}
