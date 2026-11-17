package io.github.baikoff.whattocook.recipe.service;

import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.pantry.service.PantryService;
import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeIngredient;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.repository.IngredientRepository;
import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.recipe.service.model.RecipeMatchPage;
import io.github.baikoff.whattocook.recipe.service.model.RecipeMatchResult;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionEvaluation;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeMatchingService {

	private static final Comparator<RecipeMatchResult> RESULT_ORDER =
			Comparator.comparingInt(RecipeMatchResult::matchPercentage).reversed()
					.thenComparingInt(result -> result.missingIngredients().size())
					.thenComparingInt(RecipeMatchResult::totalMinutes)
					.thenComparing(RecipeMatchResult::nameRu, String.CASE_INSENSITIVE_ORDER);

	private final RecipeRepository recipeRepository;
	private final IngredientRepository ingredientRepository;
	private final RecipeRestrictionService restrictionService;
	private final RecipeImageStorage imageStorage;
	private final IngredientAvailabilityMatcher ingredientAvailabilityMatcher;
	private final PantryService pantryService;

	public RecipeMatchingService(
			RecipeRepository recipeRepository,
			IngredientRepository ingredientRepository,
			RecipeRestrictionService restrictionService,
			RecipeImageStorage imageStorage,
			IngredientAvailabilityMatcher ingredientAvailabilityMatcher,
			PantryService pantryService) {
		this.recipeRepository = recipeRepository;
		this.ingredientRepository = ingredientRepository;
		this.restrictionService = restrictionService;
		this.imageStorage = imageStorage;
		this.ingredientAvailabilityMatcher = ingredientAvailabilityMatcher;
		this.pantryService = pantryService;
	}

	@Transactional(readOnly = true)
	public RecipeMatchPage match(List<Long> requestedIngredientIds) {
		var ingredients = ingredientRepository.findAllByOrderByNameRuAsc();
		var options = ingredients.stream()
				.map(ingredient -> new SelectionOption(ingredient.getId(), ingredient.getNameRu()))
				.toList();
		var knownIds = ingredients.stream()
				.map(Ingredient::getId)
				.collect(java.util.stream.Collectors.toSet());
		var selectedIds = normalizeSelection(requestedIngredientIds, knownIds);

		if (selectedIds.isEmpty()) {
			return new RecipeMatchPage(options, selectedIds, List.of());
		}

		var policy = restrictionService.loadPolicy();
		var pantryIngredientIds = pantryService.getIngredientIds();
		var recipes = recipeRepository.findAllByStatusNotOrderByNameRuAsc(RecipeStatus.ARCHIVED)
				.stream()
				.map(recipe -> new EvaluatedRecipe(recipe, restrictionService.evaluate(recipe, policy)))
				.filter(evaluated -> !evaluated.restriction().excluded())
				.map(evaluated -> toCandidate(
						evaluated.recipe(),
						selectedIds,
						pantryIngredientIds,
						evaluated.restriction().warnings()))
				.filter(candidate -> candidate.selectedIngredientMatchCount() > 0)
				.map(MatchCandidate::result)
				.sorted(RESULT_ORDER)
				.toList();
		return new RecipeMatchPage(options, selectedIds, recipes);
	}

	private Set<Long> normalizeSelection(List<Long> requestedIds, Set<Long> knownIds) {
		if (requestedIds == null || requestedIds.isEmpty()) {
			return Set.of();
		}

		var selectedIds = new HashSet<Long>();
		requestedIds.stream()
				.filter(Objects::nonNull)
				.filter(knownIds::contains)
				.forEach(selectedIds::add);
		return selectedIds;
	}

	private MatchCandidate toCandidate(
			Recipe recipe,
			Set<Long> selectedIds,
			Set<Long> pantryIngredientIds,
			List<RestrictionWarning> restrictionWarnings) {
		var ingredientMatch = ingredientAvailabilityMatcher.match(
				recipe.getIngredients(),
				selectedIds,
				pantryIngredientIds);

		var result = new RecipeMatchResult(
				recipe.getId(),
				recipe.getNameRu(),
				recipe.getDescription(),
				imageStorage.publicUrl(recipe.getMainImagePath()),
				ingredientMatch.matchPercentage(),
				ingredientMatch.matchedIngredientCount(),
				ingredientMatch.requiredIngredientCount(),
				ingredientMatch.missingIngredients(),
				ingredientMatch.substitutionUsages(),
				ingredientMatch.pantryIngredientNames(),
				recipe.getPreparationMinutes() + recipe.getCookingMinutes(),
				recipe.getDifficulty().getDisplayName(),
				restrictionWarnings);
		return new MatchCandidate(result, ingredientMatch.selectedIngredientMatchCount());
	}

	private record EvaluatedRecipe(Recipe recipe, RecipeRestrictionEvaluation restriction) {
	}

	private record MatchCandidate(RecipeMatchResult result, int selectedIngredientMatchCount) {
	}
}
