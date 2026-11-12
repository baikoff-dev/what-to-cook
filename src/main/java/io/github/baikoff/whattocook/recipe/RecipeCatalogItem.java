package io.github.baikoff.whattocook.recipe;

public record RecipeCatalogItem(
		Long id,
		String nameRu,
		String nameEn,
		String description,
		String mainImageUrl,
		int totalMinutes,
		int servings,
		String difficulty,
		String status) {
}
