package io.github.baikoff.whattocook.restriction.service.model;

import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;

public record RestrictionRuleItem(
		Long ingredientId,
		String ingredientName,
		RestrictionMode mode,
		String modeName,
		String note) {
}
