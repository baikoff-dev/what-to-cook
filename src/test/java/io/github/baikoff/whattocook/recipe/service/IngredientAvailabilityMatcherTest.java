package io.github.baikoff.whattocook.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.IngredientSubstitution;
import io.github.baikoff.whattocook.recipe.domain.RecipeIngredient;
import io.github.baikoff.whattocook.recipe.domain.SubstitutionType;

class IngredientAvailabilityMatcherTest {

	private final IngredientAvailabilityMatcher matcher = new IngredientAvailabilityMatcher();

	@Test
	@DisplayName("Основной ингредиент имеет приоритет и не считается вместе с заменой дважды")
	void originalIngredientHasPriorityWithoutDoubleCounting() {
		var original = ingredient(1L, "Шаосинское вино");
		var substitute = ingredient(2L, "Сухой херес");
		var recipeIngredient = recipeIngredient(original, substitution(substitute, SubstitutionType.CLOSE));

		var result = matcher.match(List.of(recipeIngredient), Set.of(1L, 2L));

		assertThat(result.matchedIngredientCount()).isEqualTo(1);
		assertThat(result.selectedIngredientMatchCount()).isEqualTo(1);
		assertThat(result.requiredIngredientCount()).isEqualTo(1);
		assertThat(result.matchPercentage()).isEqualTo(100);
		assertThat(result.missingIngredients()).isEmpty();
		assertThat(result.substitutionUsages()).isEmpty();
	}

	@ParameterizedTest(name = "Тип замены {0} полностью удовлетворяет обязательный ингредиент")
	@EnumSource(SubstitutionType.class)
	void allowedSubstitutionSatisfiesRequiredIngredient(SubstitutionType type) {
		var original = ingredient(1L, "Шаосинское вино");
		var substitute = ingredient(2L, "Сухой херес");
		var recipeIngredient = recipeIngredient(original, substitution(substitute, type));

		var result = matcher.match(List.of(recipeIngredient), Set.of(2L));

		assertThat(result.matchedIngredientCount()).isEqualTo(1);
		assertThat(result.selectedIngredientMatchCount()).isEqualTo(1);
		assertThat(result.matchPercentage()).isEqualTo(100);
		assertThat(result.missingIngredients()).isEmpty();
		assertThat(result.substitutionUsages()).singleElement().satisfies(usage -> {
			assertThat(usage.originalIngredientName()).isEqualTo("Шаосинское вино");
			assertThat(usage.substituteIngredientName()).isEqualTo("Сухой херес");
			assertThat(usage.substitutionType()).isEqualTo(type.getDisplayName());
			assertThat(usage.impactNote()).isEqualTo("Вкус немного изменится");
		});
	}

	@Test
	@DisplayName("Основной ингредиент из кладовой учитывается и помечается в результате")
	void pantryIngredientSatisfiesRequiredIngredient() {
		var salt = ingredient(1L, "Соль");
		var recipeIngredient = recipeIngredient(salt);

		var result = matcher.match(List.of(recipeIngredient), Set.of(), Set.of(1L));

		assertThat(result.matchedIngredientCount()).isEqualTo(1);
		assertThat(result.selectedIngredientMatchCount()).isZero();
		assertThat(result.matchPercentage()).isEqualTo(100);
		assertThat(result.missingIngredients()).isEmpty();
		assertThat(result.pantryIngredientNames()).containsExactly("Соль");
	}

	@Test
	@DisplayName("Явно выбранный основной ингредиент имеет приоритет над кладовой")
	void selectedIngredientHasPriorityOverPantry() {
		var salt = ingredient(1L, "Соль");
		var recipeIngredient = recipeIngredient(salt);

		var result = matcher.match(List.of(recipeIngredient), Set.of(1L), Set.of(1L));

		assertThat(result.matchedIngredientCount()).isEqualTo(1);
		assertThat(result.selectedIngredientMatchCount()).isEqualTo(1);
		assertThat(result.pantryIngredientNames()).isEmpty();
	}

	@Test
	@DisplayName("Явно выбранная замена имеет приоритет над заменой из кладовой")
	void selectedSubstitutionHasPriorityOverPantrySubstitution() {
		var original = ingredient(1L, "Арахисовое масло");
		var pantrySubstitute = ingredient(2L, "Растительное масло");
		var selectedSubstitute = ingredient(3L, "Оливковое масло");
		var recipeIngredient = recipeIngredient(
				original,
				substitution(pantrySubstitute, SubstitutionType.CLOSE),
				substitution(selectedSubstitute, SubstitutionType.ADAPTED));

		var result = matcher.match(List.of(recipeIngredient), Set.of(3L), Set.of(2L));

		assertThat(result.matchedIngredientCount()).isEqualTo(1);
		assertThat(result.selectedIngredientMatchCount()).isEqualTo(1);
		assertThat(result.pantryIngredientNames()).isEmpty();
		assertThat(result.substitutionUsages()).singleElement()
				.extracting(usage -> usage.substituteIngredientName())
				.isEqualTo("Оливковое масло");
	}

	@Test
	@DisplayName("Замена из кладовой учитывается и объясняется")
	void pantrySubstitutionSatisfiesRequiredIngredient() {
		var original = ingredient(1L, "Арахисовое масло");
		var pantrySubstitute = ingredient(2L, "Растительное масло");
		var recipeIngredient = recipeIngredient(
				original,
				substitution(pantrySubstitute, SubstitutionType.CLOSE));

		var result = matcher.match(List.of(recipeIngredient), Set.of(), Set.of(2L));

		assertThat(result.matchedIngredientCount()).isEqualTo(1);
		assertThat(result.selectedIngredientMatchCount()).isZero();
		assertThat(result.pantryIngredientNames()).containsExactly("Растительное масло");
		assertThat(result.substitutionUsages()).singleElement()
				.extracting(usage -> usage.substituteIngredientName())
				.isEqualTo("Растительное масло");
	}

	private RecipeIngredient recipeIngredient(
			Ingredient original,
			IngredientSubstitution... substitutions) {
		var recipeIngredient = mock(RecipeIngredient.class);
		when(recipeIngredient.isOptional()).thenReturn(false);
		when(recipeIngredient.getIngredient()).thenReturn(original);
		when(recipeIngredient.getSubstitutions()).thenReturn(List.of(substitutions));
		return recipeIngredient;
	}

	private IngredientSubstitution substitution(Ingredient substitute, SubstitutionType type) {
		var substitution = mock(IngredientSubstitution.class);
		when(substitution.getSubstituteIngredient()).thenReturn(substitute);
		when(substitution.getType()).thenReturn(type);
		when(substitution.getImpactNote()).thenReturn("Вкус немного изменится");
		return substitution;
	}

	private Ingredient ingredient(Long id, String name) {
		var ingredient = mock(Ingredient.class);
		when(ingredient.getId()).thenReturn(id);
		when(ingredient.getNameRu()).thenReturn(name);
		return ingredient;
	}
}
