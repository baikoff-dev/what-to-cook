package io.github.baikoff.whattocook.recipe;

import java.util.List;

public record RecipeMatchResult(
		Long id,
		String nameRu,
		String description,
		String mainImageUrl,
		int matchPercentage,
		int matchedIngredientCount,
		int requiredIngredientCount,
		List<String> missingIngredients,
		int totalMinutes,
		String difficulty) {

	public RecipeMatchResult {
		missingIngredients = List.copyOf(missingIngredients);
	}

	public boolean canCook() {
		return missingIngredients.isEmpty();
	}
}
