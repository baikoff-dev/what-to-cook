package io.github.baikoff.whattocook.assistant.service.model;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;

import java.util.List;
import java.util.Objects;

public record AssistantCriteria(
		List<Long> ingredientIds,
		Integer maxMinutes,
		Long equipmentId,
		Long cookingMethodId,
		Difficulty maxDifficulty,
		AssistantGoal goal,
		boolean surprise) {

	public AssistantCriteria {
		ingredientIds = ingredientIds == null
				? List.of()
				: ingredientIds.stream()
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		goal = goal == null ? AssistantGoal.BALANCED : goal;
	}

	public static AssistantCriteria surpriseMe() {
		return new AssistantCriteria(
				List.of(), null, null, null, null, AssistantGoal.NEW, true);
	}
}
