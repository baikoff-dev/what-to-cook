package io.github.baikoff.whattocook.recipe.domain;

public enum SubstitutionType {

	CLOSE("Близкая к классическому рецепту"),
	ADAPTED("Адаптированный вариант");

	private final String displayName;

	SubstitutionType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
