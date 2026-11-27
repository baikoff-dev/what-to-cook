package io.github.baikoff.whattocook.recipe.api;

import java.util.List;
import java.util.Set;

import io.github.baikoff.whattocook.recipe.service.model.IngredientSubstitutionUsage;
import io.github.baikoff.whattocook.recipe.service.model.RecipeMatchPage;
import io.github.baikoff.whattocook.recipe.service.model.RecipeMatchResult;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

public record RecipeMatchingResponse(
		List<IngredientOption> ingredients,
		Set<Long> selectedIngredientIds,
		List<RecipeMatch> recipes) {

	public RecipeMatchingResponse {
		ingredients = List.copyOf(ingredients);
		selectedIngredientIds = Set.copyOf(selectedIngredientIds);
		recipes = List.copyOf(recipes);
	}

	static RecipeMatchingResponse from(RecipeMatchPage page) {
		return new RecipeMatchingResponse(
				page.ingredients().stream().map(IngredientOption::from).toList(),
				page.selectedIngredientIds(),
				page.recipes().stream().map(RecipeMatch::from).toList());
	}

	public record IngredientOption(Long id, String name) {

		private static IngredientOption from(SelectionOption option) {
			return new IngredientOption(option.id(), option.name());
		}
	}

	public record RecipeMatch(
			Long id,
			String nameRu,
			String description,
			String coverUrl,
			int matchPercentage,
			int matchedIngredientCount,
			int requiredIngredientCount,
			List<String> missingIngredients,
			List<SubstitutionUsage> substitutionUsages,
			List<String> pantryIngredientNames,
			int totalMinutes,
			String difficulty,
			List<RestrictionWarningResponse> restrictionWarnings) {

		public RecipeMatch {
			missingIngredients = List.copyOf(missingIngredients);
			substitutionUsages = List.copyOf(substitutionUsages);
			pantryIngredientNames = List.copyOf(pantryIngredientNames);
			restrictionWarnings = List.copyOf(restrictionWarnings);
		}

		private static RecipeMatch from(RecipeMatchResult result) {
			return new RecipeMatch(
					result.id(),
					result.nameRu(),
					result.description(),
					result.mainImageUrl(),
					result.matchPercentage(),
					result.matchedIngredientCount(),
					result.requiredIngredientCount(),
					result.missingIngredients(),
					result.substitutionUsages().stream().map(SubstitutionUsage::from).toList(),
					result.pantryIngredientNames(),
					result.totalMinutes(),
					result.difficulty(),
					result.restrictionWarnings().stream()
							.map(RestrictionWarningResponse::from)
							.toList());
		}
	}

	public record SubstitutionUsage(
			String originalIngredientName,
			String substituteIngredientName,
			String substitutionType,
			String impactNote) {

		private static SubstitutionUsage from(IngredientSubstitutionUsage usage) {
			return new SubstitutionUsage(
					usage.originalIngredientName(),
					usage.substituteIngredientName(),
					usage.substitutionType(),
					usage.impactNote());
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
