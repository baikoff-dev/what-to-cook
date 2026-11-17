package io.github.baikoff.whattocook.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.assistant.service.model.DishRecommendation;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TelegramRecommendationServiceTest {

	private final DishRecommendationService dishRecommendationService =
			mock(DishRecommendationService.class);
	private final TelegramRecommendationService telegramRecommendationService =
			new TelegramRecommendationService(dishRecommendationService);

	@Test
	@DisplayName("Случайный рецепт преобразуется в понятную Telegram-карточку")
	void recipeIsFormattedForTelegram() {
		when(dishRecommendationService.recommend(any()))
				.thenReturn(List.of(recommendation(true, List.of("Сладкий перец"))));

		var message = telegramRecommendationService.surprise(42L);

		assertThat(message.chatId()).isEqualTo(42L);
		assertThat(message.text())
				.contains("🍽 Шакшука")
				.contains("45 мин")
				.contains("Сложность: Легко")
				.contains("Не хватает: Сладкий перец")
				.contains("⚠ Ограничения: Яйцо — есть не чаще недели");
		assertThat(message.buttons()).singleElement().satisfies(button -> {
			assertThat(button.text()).isEqualTo("Удиви ещё раз");
			assertThat(button.callbackData()).isEqualTo("SURPRISE");
		});
	}

	@Test
	@DisplayName("Карточка не сообщает о продуктах, если пользователь их не задавал")
	void ingredientSummaryIsHiddenWithoutIngredientCriteria() {
		when(dishRecommendationService.recommend(any()))
				.thenReturn(List.of(recommendation(false, List.of())));

		var message = telegramRecommendationService.surprise(42L);

		assertThat(message.text())
				.contains("🍽 Шакшука")
				.doesNotContain("Продукты:", "Не хватает:");
	}

	@Test
	@DisplayName("Пустая подборка возвращает понятное сообщение вместо ошибки")
	void emptyRecommendationListIsExplained() {
		when(dishRecommendationService.recommend(any())).thenReturn(List.of());

		var message = telegramRecommendationService.surprise(42L);

		assertThat(message.text()).contains("Пока не нашёл подходящий рецепт");
		assertThat(message.buttons()).singleElement().satisfies(button ->
				assertThat(button.callbackData()).isEqualTo("SURPRISE"));
	}

	private DishRecommendation recommendation(
			boolean ingredientsSpecified,
			List<String> missingIngredients) {
		return new DishRecommendation(
				1L,
				"Шакшука",
				"Яйца в пряном томатном соусе.",
				null,
				45,
				"Легко",
				List.of("Электроплита"),
				List.of("Тушение"),
				ingredientsSpecified,
				75,
				3,
				4,
				missingIngredients,
				List.of(),
				List.of("Яйцо", "Томат"),
				null,
				false,
				null,
				List.of("Попробовать новое"),
				List.of(new RestrictionWarning(
						"Яйцо",
						"Предупреждать",
						"есть не чаще недели",
						false,
						false)));
	}
}
