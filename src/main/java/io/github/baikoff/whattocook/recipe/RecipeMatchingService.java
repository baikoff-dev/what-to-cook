package io.github.baikoff.whattocook.recipe;

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
	private final RecipeImageStorage imageStorage;

	public RecipeMatchingService(
			RecipeRepository recipeRepository,
			IngredientRepository ingredientRepository,
			RecipeImageStorage imageStorage) {
		this.recipeRepository = recipeRepository;
		this.ingredientRepository = ingredientRepository;
		this.imageStorage = imageStorage;
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

		var recipes = recipeRepository.findAllByStatusNotOrderByNameRuAsc(RecipeStatus.ARCHIVED)
				.stream()
				.map(recipe -> toResult(recipe, selectedIds))
				.filter(result -> result.matchedIngredientCount() > 0)
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

	private RecipeMatchResult toResult(Recipe recipe, Set<Long> selectedIds) {
		var requiredIngredients = recipe.getIngredients().stream()
				.filter(recipeIngredient -> !recipeIngredient.isOptional())
				.toList();
		var missingIngredients = requiredIngredients.stream()
				.filter(recipeIngredient -> !selectedIds.contains(recipeIngredient.getIngredient().getId()))
				.map(recipeIngredient -> recipeIngredient.getIngredient().getNameRu())
				.toList();
		var requiredCount = requiredIngredients.size();
		var matchedCount = requiredCount - missingIngredients.size();
		var matchPercentage = requiredCount == 0
				? 100
				: (int) Math.round(matchedCount * 100.0 / requiredCount);

		return new RecipeMatchResult(
				recipe.getId(),
				recipe.getNameRu(),
				recipe.getDescription(),
				imageStorage.publicUrl(recipe.getMainImagePath()),
				matchPercentage,
				matchedCount,
				requiredCount,
				missingIngredients,
				recipe.getPreparationMinutes() + recipe.getCookingMinutes(),
				recipe.getDifficulty().getDisplayName());
	}
}
