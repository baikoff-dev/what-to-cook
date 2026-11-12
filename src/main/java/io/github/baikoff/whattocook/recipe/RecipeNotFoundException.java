package io.github.baikoff.whattocook.recipe;

public class RecipeNotFoundException extends RuntimeException {

	public RecipeNotFoundException(long recipeId) {
		super("Рецепт с идентификатором " + recipeId + " не найден");
	}
}
