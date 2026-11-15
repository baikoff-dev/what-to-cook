package io.github.baikoff.whattocook.recipe.domain;

public enum RecipeOrigin {

	CLASSIC("Классический"),
	PERSONAL("Личный");

	private final String displayName;

	RecipeOrigin(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
