package io.github.baikoff.whattocook.restriction.service;

import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;

import java.util.Map;

public record RestrictionPolicy(Map<Long, RestrictionRule> rules) {

	public RestrictionPolicy {
		rules = Map.copyOf(rules);
	}

	public record RestrictionRule(RestrictionMode mode, String note) {
	}
}
