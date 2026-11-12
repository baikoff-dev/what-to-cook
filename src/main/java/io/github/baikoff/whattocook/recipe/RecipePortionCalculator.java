package io.github.baikoff.whattocook.recipe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class RecipePortionCalculator {

	public static final int MIN_SERVINGS = 1;
	public static final int MAX_SERVINGS = 100;
	private static final int AMOUNT_SCALE = 3;

	public int resolveServings(Integer requestedServings, int baseServings) {
		if (baseServings < MIN_SERVINGS) {
			throw new IllegalStateException("Базовое количество порций должно быть больше нуля");
		}

		var resolvedServings = requestedServings == null ? baseServings : requestedServings;
		if (resolvedServings < MIN_SERVINGS || resolvedServings > MAX_SERVINGS) {
			throw new IllegalArgumentException("Количество порций должно быть от 1 до 100");
		}

		return resolvedServings;
	}

	public BigDecimal recalculate(BigDecimal amount, int baseServings, int requestedServings) {
		Objects.requireNonNull(amount, "Количество ингредиента обязательно");
		resolveServings(requestedServings, baseServings);

		return amount
				.multiply(BigDecimal.valueOf(requestedServings))
				.divide(BigDecimal.valueOf(baseServings), AMOUNT_SCALE, RoundingMode.HALF_UP);
	}
}
