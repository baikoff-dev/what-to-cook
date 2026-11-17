package io.github.baikoff.whattocook.recipe.service.model;

public record IngredientSubstitutionUsage(
		String originalIngredientName,
		String substituteIngredientName,
		String substitutionType,
		String impactNote) {
}
