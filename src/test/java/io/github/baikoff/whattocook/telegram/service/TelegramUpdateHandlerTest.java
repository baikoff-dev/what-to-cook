package io.github.baikoff.whattocook.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.baikoff.whattocook.telegram.model.TelegramButton;
import io.github.baikoff.whattocook.telegram.model.TelegramOutgoingMessage;
import io.github.baikoff.whattocook.telegram.model.TelegramUpdate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TelegramUpdateHandlerTest {

	private final TelegramRecommendationService recommendationService =
			mock(TelegramRecommendationService.class);
	private final TelegramUpdateHandler handler =
			new TelegramUpdateHandler(Set.of(42L), recommendationService);

	@Test
	@DisplayName("Разрешённый пользователь получает стартовое меню")
	void allowedUserReceivesStartMenu() {
		var response = handler.handle(new TelegramUpdate(1001L, 42L, 42L, "/start"));

		assertThat(response).hasValueSatisfying(message -> {
			assertThat(message.chatId()).isEqualTo(42L);
			assertThat(message.text()).contains("Что приготовить?");
			assertThat(message.buttons())
					.extracting(TelegramButton::text, TelegramButton::callbackData)
					.containsExactly(
							tuple("Удиви меня", "SURPRISE"),
							tuple("Подобрать по продуктам", "SELECT_PRODUCTS"));
		});
		verify(recommendationService).cancelProductSelection(42L);
	}

	@Test
	@DisplayName("Кнопка подбора запускает ввод продуктов")
	void productSelectionCallbackStartsProductInput() {
		var expectedMessage = new TelegramOutgoingMessage(
				42L,
				"Напиши продукты через запятую",
				List.of(new TelegramButton("Вернуться в меню", "MENU")));
		when(recommendationService.beginProductSelection(42L)).thenReturn(expectedMessage);

		var response = handler.handle(TelegramUpdate.callback(
				1004L,
				42L,
				42L,
				"callback-3",
				"SELECT_PRODUCTS"));

		assertThat(response).contains(expectedMessage);
		verify(recommendationService).beginProductSelection(42L);
	}

	@Test
	@DisplayName("Список продуктов передаётся в подбор, когда бот ожидает ввод")
	void productListIsPassedToRecommendation() {
		var expectedMessage = new TelegramOutgoingMessage(
				42L,
				"Подходящий рецепт",
				List.of(new TelegramButton("Изменить продукты", "CHANGE_PRODUCTS")));
		when(recommendationService.awaitsProductInput(42L)).thenReturn(true);
		when(recommendationService.recommendByProducts(42L, "картофель, чеснок, лук"))
				.thenReturn(expectedMessage);

		var response = handler.handle(new TelegramUpdate(
				1005L,
				42L,
				42L,
				"картофель, чеснок, лук"));

		assertThat(response).contains(expectedMessage);
		verify(recommendationService).recommendByProducts(
				42L,
				"картофель, чеснок, лук");
	}

	@Test
	@DisplayName("Кнопка SURPRISE запрашивает рекомендацию для текущего чата")
	void surpriseCallbackRequestsRecommendation() {
		var expectedMessage = new TelegramOutgoingMessage(
				42L,
				"Рекомендация",
				List.of(new TelegramButton("Удиви ещё раз", "SURPRISE")));
		when(recommendationService.surprise(42L)).thenReturn(expectedMessage);

		var response = handler.handle(TelegramUpdate.callback(
				1002L,
				42L,
				42L,
				"callback-1",
				"SURPRISE"));

		assertThat(response).contains(expectedMessage);
		verify(recommendationService).surprise(42L);
	}

	@Test
	@DisplayName("Сообщение пользователя вне списка доступа игнорируется")
	void unknownUserIsIgnored() {
		var response = handler.handle(TelegramUpdate.callback(
				1003L,
				99L,
				99L,
				"callback-2",
				"SURPRISE"));

		assertThat(response).isEmpty();
		verifyNoInteractions(recommendationService);
	}
}
