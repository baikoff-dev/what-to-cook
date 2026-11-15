package io.github.baikoff.whattocook.assistant.web;

import io.github.baikoff.whattocook.assistant.service.model.AssistantCriteria;
import io.github.baikoff.whattocook.assistant.service.model.AssistantGoal;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;

public class AssistantForm {

	private List<Long> ingredientIds = new ArrayList<>();

	@Min(value = 1, message = "Время должно быть не меньше 1 минуты")
	@Max(value = 1440, message = "Время должно быть не больше 1440 минут")
	private Integer maxMinutes;

	private Long equipmentId;

	private Long cookingMethodId;

	private Difficulty maxDifficulty;

	private AssistantGoal goal = AssistantGoal.BALANCED;

	private AssistantMode mode;

	public AssistantCriteria toCriteria() {
		if (mode == AssistantMode.SURPRISE) {
			return AssistantCriteria.surpriseMe();
		}
		return new AssistantCriteria(
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
		return mode != AssistantMode.PREFERENCES
				|| goal != AssistantGoal.USE_PRODUCTS
				|| !ingredientIds.isEmpty();
	}

	public List<Long> getIngredientIds() {
		return ingredientIds;
	}

	public void setIngredientIds(List<Long> ingredientIds) {
		this.ingredientIds = ingredientIds == null ? new ArrayList<>() : new ArrayList<>(ingredientIds);
	}

	public Integer getMaxMinutes() {
		return maxMinutes;
	}

	public void setMaxMinutes(Integer maxMinutes) {
		this.maxMinutes = maxMinutes;
	}

	public Long getEquipmentId() {
		return equipmentId;
	}

	public void setEquipmentId(Long equipmentId) {
		this.equipmentId = equipmentId;
	}

	public Long getCookingMethodId() {
		return cookingMethodId;
	}

	public void setCookingMethodId(Long cookingMethodId) {
		this.cookingMethodId = cookingMethodId;
	}

	public Difficulty getMaxDifficulty() {
		return maxDifficulty;
	}

	public void setMaxDifficulty(Difficulty maxDifficulty) {
		this.maxDifficulty = maxDifficulty;
	}

	public AssistantGoal getGoal() {
		return goal;
	}

	public void setGoal(AssistantGoal goal) {
		this.goal = goal;
	}

	public AssistantMode getMode() {
		return mode;
	}

	public void setMode(AssistantMode mode) {
		this.mode = mode;
	}
}
