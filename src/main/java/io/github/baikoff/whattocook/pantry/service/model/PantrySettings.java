package io.github.baikoff.whattocook.pantry.service.model;

import java.util.List;

public record PantrySettings(
		List<PantryOption> ingredients,
		List<String> selectedIngredientNames) {

	public PantrySettings {
		ingredients = List.copyOf(ingredients);
		selectedIngredientNames = List.copyOf(selectedIngredientNames);
	}

	public boolean hasSelectedIngredients() {
		return !selectedIngredientNames.isEmpty();
	}

	public String selectedSummary() {
		return String.join(", ", selectedIngredientNames);
	}
}
