package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.recipe.service.model.RecipeCatalogItem;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

import java.util.List;

public record RecipeCatalogResponse(List<RecipeItem> items, int total) {

	public RecipeCatalogResponse {
		items = List.copyOf(items);
	}

	static RecipeCatalogResponse from(List<RecipeCatalogItem> items) {
		var responseItems = items.stream()
				.map(RecipeItem::from)
				.toList();
		return new RecipeCatalogResponse(responseItems, responseItems.size());
	}

	public record RecipeItem(
			Long id,
			String name,
			String englishName,
			String description,
			String coverUrl,
			int totalMinutes,
			int servings,
			String difficulty,
			String status,
			String cuisine,
			String origin,
			boolean favorite,
			List<RestrictionWarningResponse> restrictionWarnings) {

		public RecipeItem {
			restrictionWarnings = List.copyOf(restrictionWarnings);
		}

		private static RecipeItem from(RecipeCatalogItem item) {
			return new RecipeItem(
					item.id(),
					item.nameRu(),
					item.nameEn(),
					item.description(),
					item.mainImageUrl(),
					item.totalMinutes(),
					item.servings(),
					item.difficulty(),
					item.status(),
					item.cuisine(),
					item.origin(),
					item.favorite(),
					item.restrictionWarnings().stream()
							.map(RestrictionWarningResponse::from)
							.toList());
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
