package io.github.baikoff.whattocook.assistant.api;

import io.github.baikoff.whattocook.assistant.service.model.AssistantCriteria;
import io.github.baikoff.whattocook.assistant.service.model.AssistantGoal;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssistantRecommendationRequest(
		@NotNull(message = "Режим подбора обязателен") Mode mode,
		List<Long> ingredientIds,
		@Min(value = 1, message = "Время должно быть не меньше 1 минуты")
		@Max(value = 1440, message = "Время должно быть не больше 1440 минут")
		Integer maxMinutes,
		Long equipmentId,
		Long cookingMethodId,
		Difficulty maxDifficulty,
		AssistantGoal goal) {

	AssistantCriteria toCriteria() {
		return mode == Mode.SURPRISE
				? AssistantCriteria.surpriseMe()
				: new AssistantCriteria(
						ingredientIds,
						maxMinutes,
						equipmentId,
						cookingMethodId,
						maxDifficulty,
						goal,
						false);
	}

	@AssertTrue(message = "Для цели «Использовать имеющиеся продукты» выбери хотя бы один продукт")
	public boolean isProductGoalValid() {
		return mode != Mode.PREFERENCES
				|| goal != AssistantGoal.USE_PRODUCTS
				|| ingredientIds != null && !ingredientIds.isEmpty();
	}

	public enum Mode {
		PREFERENCES,
		SURPRISE
	}
}
