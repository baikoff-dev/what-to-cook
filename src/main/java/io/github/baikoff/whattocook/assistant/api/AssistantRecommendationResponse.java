package io.github.baikoff.whattocook.assistant.api;

import io.github.baikoff.whattocook.assistant.service.model.DishRecommendation;
import io.github.baikoff.whattocook.recipe.service.model.IngredientSubstitutionUsage;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

import java.time.LocalDate;
import java.util.List;

public record AssistantRecommendationResponse(List<RecommendationItem> items) {

	public AssistantRecommendationResponse {
		items = List.copyOf(items);
	}

	static AssistantRecommendationResponse from(List<DishRecommendation> recommendations) {
		return new AssistantRecommendationResponse(recommendations.stream()
				.map(RecommendationItem::from)
				.toList());
	}

	public record RecommendationItem(
			Long id,
			String nameRu,
			String description,
			String coverUrl,
			int totalMinutes,
			String difficulty,
			List<String> equipment,
			List<String> cookingMethods,
			boolean ingredientsSpecified,
			int matchPercentage,
			int matchedIngredientCount,
			int requiredIngredientCount,
			List<String> missingIngredients,
			List<SubstitutionUsageResponse> substitutionUsages,
			List<String> pantryIngredientNames,
			LocalDate lastCookedOn,
			boolean favorite,
			Double averageRating,
			List<String> reasons,
			List<RestrictionWarningResponse> restrictionWarnings) {

		public RecommendationItem {
			equipment = List.copyOf(equipment);
			cookingMethods = List.copyOf(cookingMethods);
			missingIngredients = List.copyOf(missingIngredients);
			substitutionUsages = List.copyOf(substitutionUsages);
			pantryIngredientNames = List.copyOf(pantryIngredientNames);
			reasons = List.copyOf(reasons);
			restrictionWarnings = List.copyOf(restrictionWarnings);
		}

		private static RecommendationItem from(DishRecommendation recommendation) {
			return new RecommendationItem(
					recommendation.id(),
					recommendation.nameRu(),
					recommendation.description(),
					recommendation.mainImageUrl(),
					recommendation.totalMinutes(),
					recommendation.difficulty(),
					recommendation.equipment(),
					recommendation.cookingMethods(),
					recommendation.ingredientsSpecified(),
					recommendation.matchPercentage(),
					recommendation.matchedIngredientCount(),
					recommendation.requiredIngredientCount(),
					recommendation.missingIngredients(),
					recommendation.substitutionUsages().stream()
						.map(SubstitutionUsageResponse::from)
						.toList(),
					recommendation.pantryIngredientNames(),
					recommendation.lastCookedOn(),
					recommendation.favorite(),
					recommendation.averageRating(),
					recommendation.reasons(),
					recommendation.restrictionWarnings().stream()
						.map(RestrictionWarningResponse::from)
						.toList());
		}
	}

	public record SubstitutionUsageResponse(
			String originalIngredientName,
			String substituteIngredientName,
			String substitutionType,
			String impactNote) {

		private static SubstitutionUsageResponse from(IngredientSubstitutionUsage usage) {
			return new SubstitutionUsageResponse(
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
