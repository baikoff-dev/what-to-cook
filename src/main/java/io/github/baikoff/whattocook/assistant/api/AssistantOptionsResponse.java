package io.github.baikoff.whattocook.assistant.api;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.assistant.service.model.AssistantGoal;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;

import java.util.Arrays;
import java.util.List;

public record AssistantOptionsResponse(
		List<IdOption> ingredients,
		List<IdOption> equipment,
		List<IdOption> cookingMethods,
		List<EnumOption> difficulties,
		List<EnumOption> goals) {

	public AssistantOptionsResponse {
		ingredients = List.copyOf(ingredients);
		equipment = List.copyOf(equipment);
		cookingMethods = List.copyOf(cookingMethods);
		difficulties = List.copyOf(difficulties);
		goals = List.copyOf(goals);
	}

	static AssistantOptionsResponse from(DishRecommendationService recommendationService) {
		return new AssistantOptionsResponse(
				recommendationService.getIngredientOptions().stream().map(IdOption::from).toList(),
				recommendationService.getEquipmentOptions().stream().map(IdOption::from).toList(),
				recommendationService.getCookingMethodOptions().stream().map(IdOption::from).toList(),
				Arrays.stream(Difficulty.values())
						.map(value -> new EnumOption(value.name(), value.getDisplayName()))
						.toList(),
				Arrays.stream(AssistantGoal.values())
						.map(value -> new EnumOption(value.name(), value.getDisplayName()))
						.toList());
	}

	public record IdOption(Long id, String name) {

		private static IdOption from(SelectionOption option) {
			return new IdOption(option.id(), option.name());
		}
	}

	public record EnumOption(String value, String name) {
	}
}
