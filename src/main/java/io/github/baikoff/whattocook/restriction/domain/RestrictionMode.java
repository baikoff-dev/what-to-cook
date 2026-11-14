package io.github.baikoff.whattocook.restriction.domain;

public enum RestrictionMode {

	WARN("Предупреждать"),
	EXCLUDE("Исключать");

	private final String displayName;

	RestrictionMode(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
