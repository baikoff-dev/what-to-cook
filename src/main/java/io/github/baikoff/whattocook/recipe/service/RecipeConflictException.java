package io.github.baikoff.whattocook.recipe.service;

import io.github.baikoff.whattocook.recipe.domain.Recipe;

public class RecipeConflictException extends RuntimeException {

	public RecipeConflictException() {
		super("Рецепт уже был изменён в другой вкладке. Обнови страницу и повтори изменения.");
	}
}
