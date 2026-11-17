package io.github.baikoff.whattocook.recipe.service.model;

import java.util.List;

public record IngredientAvailabilityMatch(
		int matchPercentage,
		int matchedIngredientCount,
		int selectedIngredientMatchCount,
		int requiredIngredientCount,
		List<String> missingIngredients,
		List<IngredientSubstitutionUsage> substitutionUsages,
		List<String> pantryIngredientNames) {

	public IngredientAvailabilityMatch {
		missingIngredients = List.copyOf(missingIngredients);
		substitutionUsages = List.copyOf(substitutionUsages);
		pantryIngredientNames = List.copyOf(pantryIngredientNames);
	}
}
