package io.github.baikoff.whattocook.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.assistant.service.model.AssistantCriteria;
import io.github.baikoff.whattocook.assistant.service.model.DishRecommendation;
import io.github.baikoff.whattocook.image.storage.RecipeImageContent;
import io.github.baikoff.whattocook.image.storage.RecipeImageContentReader;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TelegramRecommendationServiceTest {

	private final DishRecommendationService dishRecommendationService =
			mock(DishRecommendationService.class);
	private final RecipeImageContentReader imageContentReader =
			mock(RecipeImageContentReader.class);
	private final TelegramRecommendationService telegramRecommendationService =
			new TelegramRecommendationService(dishRecommendationService, imageContentReader);

	@BeforeEach
	void resetDialog() {
		telegramRecommendationService.cancelProductSelection(42L);
	}

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

	@Test
	@DisplayName("Доступная обложка прикрепляется к карточке рецепта")
	void availableCoverIsAttachedToRecipeMessage() {
		var imageUrl = "/images/recipes/classic/shakshuka.jpg";
		when(dishRecommendationService.recommend(any())).thenReturn(List.of(
				recommendation(1L, "Шакшука", imageUrl, false, List.of())));
		when(imageContentReader.read(imageUrl)).thenReturn(Optional.of(
				new RecipeImageContent("shakshuka.jpg", "image/jpeg", new byte[] {1, 2, 3})));

		var message = telegramRecommendationService.surprise(42L);

		assertThat(message.photo()).isNotNull();
		assertThat(message.photo().fileName()).isEqualTo("shakshuka.jpg");
		assertThat(message.photo().mediaType()).isEqualTo("image/jpeg");
		assertThat(message.photo().bytes()).containsExactly(1, 2, 3);
	}

	@Test
	@DisplayName("Русские названия продуктов преобразуются в критерии общего сервиса")
	void productNamesAreConvertedToSharedRecommendationCriteria() {
		when(dishRecommendationService.getIngredientOptions()).thenReturn(List.of(
				new SelectionOption(10L, "Картофель"),
				new SelectionOption(11L, "Чеснок"),
				new SelectionOption(12L, "Репчатый лук")));
		when(dishRecommendationService.recommend(any()))
				.thenReturn(List.of(recommendation(true, List.of("Паприка"))));
		telegramRecommendationService.beginProductSelection(42L);

		var message = telegramRecommendationService.recommendByProducts(
				42L,
				" картофель, ЧЕСНОК, лук ");

		var criteriaCaptor = ArgumentCaptor.forClass(AssistantCriteria.class);
		verify(dishRecommendationService).recommend(criteriaCaptor.capture());
		assertThat(criteriaCaptor.getValue().ingredientIds()).containsExactly(10L, 11L, 12L);
		assertThat(message.text())
				.contains("Совпадение: 3 из 4 (75%)")
				.contains("Не хватает: Паприка");
		assertThat(message.buttons())
				.extracting(button -> button.callbackData())
				.containsExactly("NEXT_PRODUCT", "CHANGE_PRODUCTS");
		assertThat(telegramRecommendationService.awaitsProductInput(42L)).isFalse();
	}

	@Test
	@DisplayName("Неизвестный продукт объясняется без запуска подбора")
	void unknownProductKeepsDialogWaitingForCorrection() {
		when(dishRecommendationService.getIngredientOptions()).thenReturn(List.of(
				new SelectionOption(10L, "Картофель")));
		telegramRecommendationService.beginProductSelection(42L);

		var message = telegramRecommendationService.recommendByProducts(
				42L,
				"картофель, батат");

		assertThat(message.text())
				.contains("Не нашёл в справочнике: батат")
				.contains("отправь весь список ещё раз");
		assertThat(telegramRecommendationService.awaitsProductInput(42L)).isTrue();
		verify(dishRecommendationService).getIngredientOptions();
		verifyNoMoreInteractions(dishRecommendationService);
	}

	@Test
	@DisplayName("Кнопка другого варианта показывает следующую рекомендацию")
	void nextProductRecommendationCyclesThroughResults() {
		when(dishRecommendationService.getIngredientOptions()).thenReturn(List.of(
				new SelectionOption(10L, "Картофель")));
		when(dishRecommendationService.recommend(any())).thenReturn(List.of(
				recommendation(1L, "Шакшука", true, List.of("Паприка")),
				recommendation(2L, "Белорусские драники", true, List.of("Лук"))));
		telegramRecommendationService.beginProductSelection(42L);
		telegramRecommendationService.recommendByProducts(42L, "картофель");

		var message = telegramRecommendationService.nextProductRecommendation(42L);

		assertThat(message.text())
				.contains("🍽 Белорусские драники")
				.doesNotContain("🍽 Шакшука");
	}

	private DishRecommendation recommendation(
			boolean ingredientsSpecified,
			List<String> missingIngredients) {
		return recommendation(1L, "Шакшука", ingredientsSpecified, missingIngredients);
	}

	private DishRecommendation recommendation(
			long id,
			String name,
			boolean ingredientsSpecified,
			List<String> missingIngredients) {
		return recommendation(id, name, null, ingredientsSpecified, missingIngredients);
	}

	private DishRecommendation recommendation(
			long id,
			String name,
			String mainImageUrl,
			boolean ingredientsSpecified,
			List<String> missingIngredients) {
		return new DishRecommendation(
				id,
				name,
				"Яйца в пряном томатном соусе.",
				mainImageUrl,
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
