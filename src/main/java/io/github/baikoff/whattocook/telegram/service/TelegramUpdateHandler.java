package io.github.baikoff.whattocook.telegram.service;

import io.github.baikoff.whattocook.telegram.model.TelegramButton;
import io.github.baikoff.whattocook.telegram.model.TelegramOutgoingMessage;
import io.github.baikoff.whattocook.telegram.model.TelegramUpdate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TelegramUpdateHandler {

	private static final String SURPRISE_CALLBACK = "SURPRISE";
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
			return Optional.of(startMessage(update.chatId()));
		}
		if (SURPRISE_CALLBACK.equals(update.callbackData())) {
			return Optional.of(recommendationService.surprise(update.chatId()));
		}
		return Optional.empty();
	}

	private TelegramOutgoingMessage startMessage(long chatId) {
		return new TelegramOutgoingMessage(
				chatId,
				WELCOME_TEXT,
				List.of(new TelegramButton("Удиви меня", SURPRISE_CALLBACK)));
	}

	private boolean isStartCommand(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		var command = text.strip().split("\\s+", 2)[0];
		return command.equals("/start") || command.startsWith("/start@");
	}
}
