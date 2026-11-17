package io.github.baikoff.whattocook.recipe.service;

import io.github.baikoff.whattocook.recipe.domain.RecipeIngredient;
import io.github.baikoff.whattocook.recipe.domain.IngredientSubstitution;
import io.github.baikoff.whattocook.recipe.service.model.IngredientAvailabilityMatch;
import io.github.baikoff.whattocook.recipe.service.model.IngredientSubstitutionUsage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class IngredientAvailabilityMatcher {

	public IngredientAvailabilityMatch match(
			List<RecipeIngredient> recipeIngredients,
			Set<Long> selectedIngredientIds) {
		return match(recipeIngredients, selectedIngredientIds, Set.of());
	}

	public IngredientAvailabilityMatch match(
			List<RecipeIngredient> recipeIngredients,
			Set<Long> selectedIngredientIds,
			Set<Long> pantryIngredientIds) {
		var missingIngredients = new ArrayList<String>();
		var substitutionUsages = new ArrayList<IngredientSubstitutionUsage>();
		var pantryIngredientNames = new LinkedHashSet<String>();
		var requiredIngredientCount = 0;
		var matchedIngredientCount = 0;
		var selectedIngredientMatchCount = 0;

		for (var recipeIngredient : recipeIngredients) {
			if (recipeIngredient.isOptional()) {
				continue;
			}

			requiredIngredientCount++;
			if (selectedIngredientIds.contains(recipeIngredient.getIngredient().getId())) {
				matchedIngredientCount++;
				selectedIngredientMatchCount++;
				continue;
			}
			if (pantryIngredientIds.contains(recipeIngredient.getIngredient().getId())) {
				matchedIngredientCount++;
				pantryIngredientNames.add(recipeIngredient.getIngredient().getNameRu());
				continue;
			}

			var selectedSubstitution = findSubstitution(recipeIngredient, selectedIngredientIds);
			if (selectedSubstitution.isPresent()) {
				matchedIngredientCount++;
				selectedIngredientMatchCount++;
				substitutionUsages.add(toUsage(recipeIngredient, selectedSubstitution.get()));
				continue;
			}

			var pantrySubstitution = findSubstitution(recipeIngredient, pantryIngredientIds);
			if (pantrySubstitution.isPresent()) {
				var substitution = pantrySubstitution.get();
				matchedIngredientCount++;
				pantryIngredientNames.add(substitution.getSubstituteIngredient().getNameRu());
				substitutionUsages.add(toUsage(recipeIngredient, substitution));
				continue;
			}

			missingIngredients.add(recipeIngredient.getIngredient().getNameRu());
		}

		var matchPercentage = requiredIngredientCount == 0
				? 100
				: (int) Math.round(matchedIngredientCount * 100.0 / requiredIngredientCount);
		return new IngredientAvailabilityMatch(
				matchPercentage,
				matchedIngredientCount,
				selectedIngredientMatchCount,
				requiredIngredientCount,
				missingIngredients,
				substitutionUsages,
				List.copyOf(pantryIngredientNames));
	}

	private Optional<IngredientSubstitution> findSubstitution(
			RecipeIngredient recipeIngredient,
			Set<Long> availableIngredientIds) {
		return recipeIngredient.getSubstitutions().stream()
				.filter(substitution -> availableIngredientIds.contains(
						substitution.getSubstituteIngredient().getId()))
				.findFirst();
	}

	private IngredientSubstitutionUsage toUsage(
			RecipeIngredient recipeIngredient,
			IngredientSubstitution substitution) {
		return new IngredientSubstitutionUsage(
				recipeIngredient.getIngredient().getNameRu(),
				substitution.getSubstituteIngredient().getNameRu(),
				substitution.getType().getDisplayName(),
				substitution.getImpactNote());
	}
}
