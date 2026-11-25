package io.github.baikoff.whattocook.restriction.api;

import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionService;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionRuleItem;

import java.util.Arrays;
import java.util.List;

public record IngredientRestrictionPageResponse(
		List<IngredientOption> ingredients,
		List<ModeOption> modes,
		List<Rule> rules,
		int total) {

	public IngredientRestrictionPageResponse {
		ingredients = List.copyOf(ingredients);
		modes = List.copyOf(modes);
		rules = List.copyOf(rules);
	}

	static IngredientRestrictionPageResponse from(RecipeRestrictionService service) {
		var rules = service.getRules().stream().map(Rule::from).toList();
		return new IngredientRestrictionPageResponse(
				service.getIngredientOptions().stream().map(IngredientOption::from).toList(),
				Arrays.stream(RestrictionMode.values()).map(ModeOption::from).toList(),
				rules,
				rules.size());
	}

	public record IngredientOption(long id, String name) {

		private static IngredientOption from(SelectionOption option) {
			return new IngredientOption(option.id(), option.name());
		}
	}

	public record ModeOption(String value, String name) {

		private static ModeOption from(RestrictionMode mode) {
			return new ModeOption(mode.name(), mode.getDisplayName());
		}
	}

	public record Rule(
			long ingredientId,
			String ingredientName,
			RestrictionMode mode,
			String modeName,
			String note) {

		private static Rule from(RestrictionRuleItem item) {
			return new Rule(
					item.ingredientId(),
					item.ingredientName(),
					item.mode(),
					item.modeName(),
					item.note());
		}
	}
}
