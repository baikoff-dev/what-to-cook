package io.github.baikoff.whattocook.restriction.service;

import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

import java.util.List;

public record RecipeRestrictionEvaluation(
		boolean excluded,
		List<RestrictionWarning> warnings) {

	public RecipeRestrictionEvaluation {
		warnings = List.copyOf(warnings);
	}
}
