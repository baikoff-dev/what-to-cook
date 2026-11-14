package io.github.baikoff.whattocook.recipe.domain;

public enum MeasurementUnit {

	GRAM("г"),
	KILOGRAM("кг"),
	MILLILITER("мл"),
	LITER("л"),
	PIECE("шт."),
	TEASPOON("ч. л."),
	TABLESPOON("ст. л."),
	PINCH("щепотка"),
	CLOVE("зубчик"),
	SLICE("ломтик"),
	BUNCH("пучок"),
	PACKAGE("упаковка");

	private final String displayName;

	MeasurementUnit(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
