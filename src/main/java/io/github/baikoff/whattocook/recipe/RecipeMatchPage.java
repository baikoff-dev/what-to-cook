package io.github.baikoff.whattocook.recipe;

import java.util.List;
import java.util.Set;

public record RecipeMatchPage(
		List<SelectionOption> ingredients,
		Set<Long> selectedIngredientIds,
		List<RecipeMatchResult> recipes) {

	public RecipeMatchPage {
		ingredients = List.copyOf(ingredients);
		selectedIngredientIds = Set.copyOf(selectedIngredientIds);
		recipes = List.copyOf(recipes);
	}

	public boolean hasSelection() {
		return !selectedIngredientIds.isEmpty();
	}
}
