package io.github.baikoff.whattocook.recipe;

public enum Difficulty {

	EASY("Легко"),
	MEDIUM("Средне"),
	HARD("Сложно");

	private final String displayName;

	Difficulty(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
