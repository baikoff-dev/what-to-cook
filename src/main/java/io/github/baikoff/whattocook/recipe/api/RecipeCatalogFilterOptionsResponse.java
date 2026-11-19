package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.RecipeOrigin;
import io.github.baikoff.whattocook.recipe.service.RecipeManagementService;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;

import java.util.Arrays;
import java.util.List;

public record RecipeCatalogFilterOptionsResponse(
		List<EnumOption> difficulties,
		List<IdOption> equipment,
		List<IdOption> cookingMethods,
		List<IdOption> cuisines,
		List<EnumOption> origins) {

	public RecipeCatalogFilterOptionsResponse {
		difficulties = List.copyOf(difficulties);
		equipment = List.copyOf(equipment);
		cookingMethods = List.copyOf(cookingMethods);
		cuisines = List.copyOf(cuisines);
		origins = List.copyOf(origins);
	}

	static RecipeCatalogFilterOptionsResponse from(RecipeManagementService service) {
		return new RecipeCatalogFilterOptionsResponse(
				Arrays.stream(Difficulty.values())
						.map(value -> new EnumOption(value.name(), value.getDisplayName()))
						.toList(),
				service.getEquipmentOptions().stream().map(IdOption::from).toList(),
				service.getCookingMethodOptions().stream().map(IdOption::from).toList(),
				service.getCuisineOptions().stream().map(IdOption::from).toList(),
				Arrays.stream(RecipeOrigin.values())
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
