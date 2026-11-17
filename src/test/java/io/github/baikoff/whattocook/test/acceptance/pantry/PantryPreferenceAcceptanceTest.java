package io.github.baikoff.whattocook.test.acceptance.pantry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class PantryPreferenceAcceptanceTest extends AbstractIntegrationTest {

	private static final String DEFAULT_PANTRY_SUMMARY =
			"Сейчас всегда учитываются: Вода, Растительное масло, Соль";
	private static final String RECIPE_NAME = "Картофель, запечённый в духовке";

	@Test
	@DisplayName("Страница базовой кладовой показывает продукты по умолчанию")
	void pantryPageShowsDefaultIngredients() throws IOException, InterruptedException {
		var response = get("/preferences/pantry");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("Базовая кладовая")
				.contains(DEFAULT_PANTRY_SUMMARY);
	}

	@Test
	@DisplayName("Пользователь может изменить состав базовой кладовой")
	void pantrySettingsCanBeChanged() throws IOException, InterruptedException {
		try {
			var saveResponse = postForm(
					"/preferences/pantry",
					pantryForm("Вода", "Соль"));
			var pageResponse = get("/preferences/pantry");

			assertThat(saveResponse.statusCode()).isEqualTo(302);
			assertThat(saveResponse.headers().firstValue("Location"))
					.hasValueSatisfying(location -> assertThat(location).contains("/preferences/pantry"));
			assertThat(pageResponse.statusCode()).isEqualTo(200);
			assertThat(pageResponse.body())
					.contains("Сейчас всегда учитываются: Вода, Соль")
					.doesNotContain(DEFAULT_PANTRY_SUMMARY);
		} finally {
			postForm(
					"/preferences/pantry",
					pantryForm("Вода", "Растительное масло", "Соль"));
		}
	}

	@Test
	@DisplayName("Подбор автоматически учитывает продукты из базовой кладовой")
	void matchingPageCountsPantryIngredients() throws IOException, InterruptedException {
		var potatoId = ingredientIds("Картофель").getFirst();

		var response = get("/recipes/match?ingredientIds=" + potatoId);

		assertThat(response.statusCode()).isEqualTo(200);
		assertPantryMatch(response.body());
	}

	@Test
	@DisplayName("Помощник автоматически учитывает продукты из базовой кладовой")
	void assistantCountsPantryIngredients() throws IOException, InterruptedException {
		var potatoId = ingredientIds("Картофель").getFirst();

		var response = get("/assistant?ingredientIds=" + potatoId
				+ "&goal=USE_PRODUCTS&mode=PREFERENCES");

		assertThat(response.statusCode()).isEqualTo(200);
		assertPantryMatch(response.body());
	}

	private void assertPantryMatch(String responseBody) {
		assertThat(responseBody)
				.contains(RECIPE_NAME)
				.contains("60%")
				.contains("Есть 3 из 5 обязательных продуктов")
				.contains("Из базовой кладовой: Растительное масло, Соль");
	}

	private String pantryForm(String... ingredientNames) {
		var ids = ingredientIds(ingredientNames);
		return ids.stream()
				.map(id -> "ingredientIds=" + id)
				.collect(Collectors.joining("&"));
	}
}
