package io.github.baikoff.whattocook.recipe;

public class RecipeConflictException extends RuntimeException {

	public RecipeConflictException() {
		super("Рецепт уже был изменён в другой вкладке. Обнови страницу и повтори изменения.");
	}
}
