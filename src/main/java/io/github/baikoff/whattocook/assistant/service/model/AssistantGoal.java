package io.github.baikoff.whattocook.assistant.service.model;

public enum AssistantGoal {

	BALANCED("Без разницы"),
	USE_PRODUCTS("Использовать имеющиеся продукты"),
	QUICK("Приготовить побыстрее"),
	NEW("Попробовать новое"),
	FAVORITE("Выбрать любимое");

	private final String displayName;

	AssistantGoal(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
