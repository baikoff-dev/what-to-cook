package io.github.baikoff.whattocook.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RecipePortionCalculatorTest {

	private final RecipePortionCalculator calculator = new RecipePortionCalculator();

	@Test
	void recalculatesAndRoundsAmountToThreeDecimalPlaces() {
		var result = calculator.recalculate(BigDecimal.ONE, 3, 2);

		assertThat(result).isEqualByComparingTo("0.667");
	}

	@Test
	void usesBaseServingsWhenRequestedValueIsMissing() {
		assertThat(calculator.resolveServings(null, 4)).isEqualTo(4);
	}

	@Test
	void rejectsRequestedServingsOutsideSupportedRange() {
		assertThatThrownBy(() -> calculator.resolveServings(0, 2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Количество порций должно быть от 1 до 100");
		assertThatThrownBy(() -> calculator.resolveServings(101, 2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Количество порций должно быть от 1 до 100");
	}
}
