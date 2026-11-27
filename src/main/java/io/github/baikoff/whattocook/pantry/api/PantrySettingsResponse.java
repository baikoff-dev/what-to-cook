package io.github.baikoff.whattocook.pantry.api;

import java.util.List;

import io.github.baikoff.whattocook.pantry.service.model.PantryOption;
import io.github.baikoff.whattocook.pantry.service.model.PantrySettings;

public record PantrySettingsResponse(
		List<IngredientOption> ingredients,
		List<String> selectedIngredientNames) {

	public PantrySettingsResponse {
		ingredients = List.copyOf(ingredients);
		selectedIngredientNames = List.copyOf(selectedIngredientNames);
	}

	static PantrySettingsResponse from(PantrySettings settings) {
		return new PantrySettingsResponse(
				settings.ingredients().stream().map(IngredientOption::from).toList(),
				settings.selectedIngredientNames());
	}

	public record IngredientOption(Long id, String name, boolean selected) {

		private static IngredientOption from(PantryOption option) {
			return new IngredientOption(option.id(), option.name(), option.selected());
		}
	}
}
