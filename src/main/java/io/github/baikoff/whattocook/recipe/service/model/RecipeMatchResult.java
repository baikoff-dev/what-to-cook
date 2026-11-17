package io.github.baikoff.whattocook.recipe.service.model;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

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
		List<IngredientSubstitutionUsage> substitutionUsages,
		List<String> pantryIngredientNames,
		int totalMinutes,
		String difficulty,
		List<RestrictionWarning> restrictionWarnings) {

	public RecipeMatchResult {
		missingIngredients = List.copyOf(missingIngredients);
		substitutionUsages = List.copyOf(substitutionUsages);
		pantryIngredientNames = List.copyOf(pantryIngredientNames);
		restrictionWarnings = List.copyOf(restrictionWarnings);
	}

	public boolean canCook() {
		return missingIngredients.isEmpty();
	}

	public String pantrySummary() {
		return String.join(", ", pantryIngredientNames);
	}
}
