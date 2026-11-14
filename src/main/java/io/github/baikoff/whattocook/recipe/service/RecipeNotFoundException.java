package io.github.baikoff.whattocook.recipe.service;

import io.github.baikoff.whattocook.recipe.domain.Recipe;

public class RecipeNotFoundException extends RuntimeException {

	public RecipeNotFoundException(long recipeId) {
		super("Рецепт с идентификатором " + recipeId + " не найден");
	}
}
