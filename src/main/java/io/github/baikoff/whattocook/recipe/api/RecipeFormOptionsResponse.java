package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.MeasurementUnit;
import io.github.baikoff.whattocook.recipe.domain.RecipeOrigin;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.domain.SubstitutionType;
import io.github.baikoff.whattocook.recipe.service.RecipeManagementService;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;

import java.util.Arrays;
import java.util.List;

public record RecipeFormOptionsResponse(
		List<EnumOption> difficulties,
		List<EnumOption> statuses,
		List<EnumOption> origins,
		List<EnumOption> measurementUnits,
		List<EnumOption> substitutionTypes,
		List<IdOption> equipment,
		List<IdOption> cookingMethods,
		List<IdOption> cuisines) {

	public RecipeFormOptionsResponse {
		difficulties = List.copyOf(difficulties);
		statuses = List.copyOf(statuses);
		origins = List.copyOf(origins);
		measurementUnits = List.copyOf(measurementUnits);
		substitutionTypes = List.copyOf(substitutionTypes);
		equipment = List.copyOf(equipment);
		cookingMethods = List.copyOf(cookingMethods);
		cuisines = List.copyOf(cuisines);
	}

	static RecipeFormOptionsResponse from(RecipeManagementService service) {
		return new RecipeFormOptionsResponse(
				enumOptions(Difficulty.values(), Difficulty::getDisplayName),
				Arrays.stream(RecipeStatus.values())
						.filter(status -> status != RecipeStatus.ARCHIVED)
						.map(status -> new EnumOption(status.name(), status.getDisplayName()))
						.toList(),
				enumOptions(RecipeOrigin.values(), RecipeOrigin::getDisplayName),
				enumOptions(MeasurementUnit.values(), MeasurementUnit::getDisplayName),
				enumOptions(SubstitutionType.values(), SubstitutionType::getDisplayName),
				service.getEquipmentOptions().stream().map(IdOption::from).toList(),
				service.getCookingMethodOptions().stream().map(IdOption::from).toList(),
				service.getCuisineOptions().stream().map(IdOption::from).toList());
	}

	private static <E extends Enum<E>> List<EnumOption> enumOptions(
			E[] values,
			java.util.function.Function<E, String> displayName) {
		return Arrays.stream(values)
				.map(value -> new EnumOption(value.name(), displayName.apply(value)))
				.toList();
	}

	public record IdOption(long id, String name) {

		private static IdOption from(SelectionOption option) {
			return new IdOption(option.id(), option.name());
		}
	}

	public record EnumOption(String value, String name) {
	}
}
