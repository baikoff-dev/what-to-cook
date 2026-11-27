package io.github.baikoff.whattocook.pantry.api;

import java.util.List;

public record PantrySettingsRequest(List<Long> ingredientIds) {

	public PantrySettingsRequest {
		ingredientIds = ingredientIds == null ? List.of() : List.copyOf(ingredientIds);
	}
}
