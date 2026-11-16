package io.github.baikoff.whattocook.recipe.service.model;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.Equipment;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

import java.util.List;

public record RecipeDetails(
		Long id,
		String nameRu,
		String nameEn,
		String description,
		String mainImageUrl,
		int baseServings,
		int servings,
		int preparationMinutes,
		int cookingMinutes,
		int totalMinutes,
		String difficulty,
		String status,
		String cuisine,
		String origin,
		String sourceName,
		String sourceUrl,
		String videoUrl,
		String personalNotes,
		List<IngredientLine> ingredients,
		List<Step> steps,
		List<String> equipment,
		List<String> cookingMethods,
		boolean favorite,
		boolean excludedByRestriction,
		List<RestrictionWarning> restrictionWarnings) {

	public RecipeDetails {
		restrictionWarnings = List.copyOf(restrictionWarnings);
	}

	public record IngredientLine(
			String name,
			String amount,
			String unit,
			boolean optional,
			List<Substitution> substitutions) {

		public IngredientLine {
			substitutions = List.copyOf(substitutions);
		}
	}

	public record Substitution(
			String ingredientName,
			String amount,
			String unit,
			String type,
			String impactNote) {
	}

	public record Step(
			int number,
			String instruction,
			String imageUrl) {
	}
}
