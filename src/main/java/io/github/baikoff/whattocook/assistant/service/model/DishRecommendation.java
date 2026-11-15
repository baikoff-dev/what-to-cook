package io.github.baikoff.whattocook.assistant.service.model;

import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

import java.time.LocalDate;
import java.util.List;

public record DishRecommendation(
		Long id,
		String nameRu,
		String description,
		String mainImageUrl,
		int totalMinutes,
		String difficulty,
		List<String> equipment,
		List<String> cookingMethods,
		boolean ingredientsSpecified,
		int matchPercentage,
		int matchedIngredientCount,
		int requiredIngredientCount,
		List<String> missingIngredients,
		LocalDate lastCookedOn,
		boolean favorite,
		Double averageRating,
		List<String> reasons,
		List<RestrictionWarning> restrictionWarnings) {

	public DishRecommendation {
		equipment = List.copyOf(equipment);
		cookingMethods = List.copyOf(cookingMethods);
		missingIngredients = List.copyOf(missingIngredients);
		reasons = List.copyOf(reasons);
		restrictionWarnings = List.copyOf(restrictionWarnings);
	}

	public boolean canCook() {
		return ingredientsSpecified && missingIngredients.isEmpty();
	}
}
