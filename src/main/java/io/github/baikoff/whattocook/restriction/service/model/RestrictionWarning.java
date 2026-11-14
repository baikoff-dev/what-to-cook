package io.github.baikoff.whattocook.restriction.service.model;

public record RestrictionWarning(
		String ingredientName,
		String modeName,
		String note,
		boolean optional,
		boolean excluding) {
}
