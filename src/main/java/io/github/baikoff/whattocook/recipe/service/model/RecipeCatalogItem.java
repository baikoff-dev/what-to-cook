package io.github.baikoff.whattocook.recipe.service.model;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

import java.util.List;

public record RecipeCatalogItem(
		Long id,
		String nameRu,
		String nameEn,
		String description,
		String mainImageUrl,
		int totalMinutes,
		int servings,
		String difficulty,
		String status,
		boolean favorite,
		List<RestrictionWarning> restrictionWarnings) {

	public RecipeCatalogItem {
		restrictionWarnings = List.copyOf(restrictionWarnings);
	}
}
