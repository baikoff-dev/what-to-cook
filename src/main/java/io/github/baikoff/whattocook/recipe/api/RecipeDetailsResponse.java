package io.github.baikoff.whattocook.recipe.api;

import java.util.List;

import io.github.baikoff.whattocook.recipe.service.model.RecipeDetails;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

public record RecipeDetailsResponse(
		Long id,
		String name,
		String englishName,
		String description,
		String coverUrl,
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
		List<IngredientResponse> ingredients,
		List<StepResponse> steps,
		List<String> equipment,
		List<String> cookingMethods,
		boolean favorite,
		boolean excludedByRestriction,
		List<RestrictionWarningResponse> restrictionWarnings) {

	public RecipeDetailsResponse {
		ingredients = List.copyOf(ingredients);
		steps = List.copyOf(steps);
		equipment = List.copyOf(equipment);
		cookingMethods = List.copyOf(cookingMethods);
		restrictionWarnings = List.copyOf(restrictionWarnings);
	}

	static RecipeDetailsResponse from(RecipeDetails details) {
		return new RecipeDetailsResponse(
				details.id(),
				details.nameRu(),
				details.nameEn(),
				details.description(),
				details.mainImageUrl(),
				details.baseServings(),
				details.servings(),
				details.preparationMinutes(),
				details.cookingMinutes(),
				details.totalMinutes(),
				details.difficulty(),
				details.status(),
				details.cuisine(),
				details.origin(),
				details.sourceName(),
				details.sourceUrl(),
				details.videoUrl(),
				details.personalNotes(),
				details.ingredients().stream().map(IngredientResponse::from).toList(),
				details.steps().stream().map(StepResponse::from).toList(),
				details.equipment(),
				details.cookingMethods(),
				details.favorite(),
				details.excludedByRestriction(),
				details.restrictionWarnings().stream()
						.map(RestrictionWarningResponse::from)
						.toList());
	}

	public record IngredientResponse(
			String name,
			String amount,
			String unit,
			boolean optional,
			List<SubstitutionResponse> substitutions) {

		public IngredientResponse {
			substitutions = List.copyOf(substitutions);
		}

		private static IngredientResponse from(RecipeDetails.IngredientLine ingredient) {
			return new IngredientResponse(
					ingredient.name(),
					ingredient.amount(),
					ingredient.unit(),
					ingredient.optional(),
					ingredient.substitutions().stream()
							.map(SubstitutionResponse::from)
							.toList());
		}
	}

	public record SubstitutionResponse(
			String ingredientName,
			String amount,
			String unit,
			String type,
			String impactNote) {

		private static SubstitutionResponse from(RecipeDetails.Substitution substitution) {
			return new SubstitutionResponse(
					substitution.ingredientName(),
					substitution.amount(),
					substitution.unit(),
					substitution.type(),
					substitution.impactNote());
		}
	}

	public record StepResponse(int number, String instruction, String imageUrl) {

		private static StepResponse from(RecipeDetails.Step step) {
			return new StepResponse(step.number(), step.instruction(), step.imageUrl());
		}
	}

	public record RestrictionWarningResponse(
			String ingredientName,
			String modeName,
			String note,
			boolean optional,
			boolean excluding) {

		private static RestrictionWarningResponse from(RestrictionWarning warning) {
			return new RestrictionWarningResponse(
					warning.ingredientName(),
					warning.modeName(),
					warning.note(),
					warning.optional(),
					warning.excluding());
		}
	}
}
