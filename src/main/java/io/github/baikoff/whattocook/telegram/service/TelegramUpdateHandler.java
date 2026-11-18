package io.github.baikoff.whattocook.telegram.service;

import io.github.baikoff.whattocook.telegram.model.TelegramButton;
import io.github.baikoff.whattocook.telegram.model.TelegramOutgoingMessage;
import io.github.baikoff.whattocook.telegram.model.TelegramUpdate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TelegramUpdateHandler {

	private static final String WELCOME_TEXT = """
			Что приготовить?

			Я помогу выбрать блюдо из твоего каталога рецептов.
			""";

	private final Set<Long> allowedUserIds;
	private final TelegramRecommendationService recommendationService;

	public TelegramUpdateHandler(
			Set<Long> allowedUserIds,
			TelegramRecommendationService recommendationService) {
		this.allowedUserIds = Set.copyOf(allowedUserIds);
		this.recommendationService = recommendationService;
	}

	public Optional<TelegramOutgoingMessage> handle(TelegramUpdate update) {
		if (!allowedUserIds.contains(update.userId())) {
			return Optional.empty();
		}
		if (isStartCommand(update.text())) {
			recommendationService.cancelProductSelection(update.chatId());
			return Optional.of(startMessage(update.chatId()));
		}
		if (TelegramRecommendationService.SURPRISE_CALLBACK.equals(update.callbackData())) {
			return Optional.of(recommendationService.surprise(update.chatId()));
		}
		if (TelegramRecommendationService.SELECT_PRODUCTS_CALLBACK.equals(update.callbackData())
				|| TelegramRecommendationService.CHANGE_PRODUCTS_CALLBACK.equals(update.callbackData())) {
			return Optional.of(recommendationService.beginProductSelection(update.chatId()));
		}
		if (TelegramRecommendationService.NEXT_PRODUCT_CALLBACK.equals(update.callbackData())) {
			return Optional.of(recommendationService.nextProductRecommendation(update.chatId()));
		}
		if (TelegramRecommendationService.MENU_CALLBACK.equals(update.callbackData())) {
			recommendationService.cancelProductSelection(update.chatId());
			return Optional.of(startMessage(update.chatId()));
		}
		if (recommendationService.awaitsProductInput(update.chatId()) && hasText(update.text())) {
			return Optional.of(recommendationService.recommendByProducts(update.chatId(), update.text()));
		}
		return Optional.empty();
	}

	private TelegramOutgoingMessage startMessage(long chatId) {
		return new TelegramOutgoingMessage(
				chatId,
				WELCOME_TEXT,
				List.of(
						new TelegramButton(
								"Удиви меня",
								TelegramRecommendationService.SURPRISE_CALLBACK),
						new TelegramButton(
								"Подобрать по продуктам",
								TelegramRecommendationService.SELECT_PRODUCTS_CALLBACK)));
	}

	private boolean hasText(String text) {
		return text != null && !text.isBlank();
	}

	private boolean isStartCommand(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		var command = text.strip().split("\\s+", 2)[0];
		return command.equals("/start") || command.startsWith("/start@");
	}
}
