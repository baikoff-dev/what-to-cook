package io.github.baikoff.whattocook.recipe;

public enum RecipeStatus {

	UNTESTED("Не проверен"),
	TESTED("Проверен"),
	NEEDS_CHANGES("Нужно доработать"),
	ARCHIVED("В архиве");

	private final String displayName;

	RecipeStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
