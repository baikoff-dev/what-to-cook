package io.github.baikoff.whattocook.telegram.client;

import io.github.baikoff.whattocook.telegram.config.TelegramProperties;
import io.github.baikoff.whattocook.telegram.model.TelegramOutgoingMessage;
import io.github.baikoff.whattocook.telegram.model.TelegramUpdate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.client.RestClient;

public class TelegramBotApiClient {

	private static final int LONG_POLLING_TIMEOUT_SECONDS = 25;

	private final RestClient restClient;

	public TelegramBotApiClient(TelegramProperties properties) {
		var apiBaseUrl = properties.getApiBaseUrl().replaceAll("/+$", "");
		this.restClient = RestClient.builder()
				.baseUrl(apiBaseUrl + "/bot" + properties.getToken())
				.build();
	}

	public List<TelegramUpdate> getUpdates(long offset) {
		var request = new LinkedHashMap<String, Object>();
		request.put("offset", offset);
		request.put("timeout", LONG_POLLING_TIMEOUT_SECONDS);
		request.put("allowed_updates", List.of("message", "callback_query"));

		var response = restClient.post()
				.uri("/getUpdates")
				.body(request)
				.retrieve()
				.body(Map.class);

		return readUpdates(response);
	}

	public void answerCallbackQuery(String callbackQueryId) {
		restClient.post()
				.uri("/answerCallbackQuery")
				.body(Map.of("callback_query_id", callbackQueryId))
				.retrieve()
				.toBodilessEntity();
	}

	public void sendMessage(TelegramOutgoingMessage message) {
		var buttons = message.buttons().stream()
				.map(button -> Map.of(
						"text", button.text(),
						"callback_data", button.callbackData()))
				.toList();
		var request = new LinkedHashMap<String, Object>();
		request.put("chat_id", message.chatId());
		request.put("text", message.text());
		request.put("reply_markup", Map.of("inline_keyboard", List.of(buttons)));

		restClient.post()
				.uri("/sendMessage")
				.body(request)
				.retrieve()
				.toBodilessEntity();
	}

	private List<TelegramUpdate> readUpdates(Map<?, ?> response) {
		if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
			throw new IllegalStateException("Telegram API вернул неуспешный ответ");
		}
		if (!(response.get("result") instanceof List<?> results)) {
			return List.of();
		}

		var updates = new ArrayList<TelegramUpdate>();
		for (var item : results) {
			readUpdate(item).ifPresent(updates::add);
		}
		return List.copyOf(updates);
	}

	private Optional<TelegramUpdate> readUpdate(Object item) {
		if (!(item instanceof Map<?, ?> update)
				|| !(update.get("update_id") instanceof Number updateId)) {
			return Optional.empty();
		}

		var messageUpdate = readMessageUpdate(updateId.longValue(), update.get("message"));
		if (messageUpdate.isPresent()) {
			return messageUpdate;
		}
		return readCallbackUpdate(updateId.longValue(), update.get("callback_query"));
	}

	private Optional<TelegramUpdate> readMessageUpdate(long updateId, Object item) {
		if (!(item instanceof Map<?, ?> message)
				|| !(message.get("from") instanceof Map<?, ?> from)
				|| !(from.get("id") instanceof Number userId)
				|| !(message.get("chat") instanceof Map<?, ?> chat)
				|| !(chat.get("id") instanceof Number chatId)
				|| !(message.get("text") instanceof String text)) {
			return Optional.empty();
		}

		return Optional.of(new TelegramUpdate(
				updateId,
				userId.longValue(),
				chatId.longValue(),
				text));
	}

	private Optional<TelegramUpdate> readCallbackUpdate(long updateId, Object item) {
		if (!(item instanceof Map<?, ?> callback)
				|| !(callback.get("id") instanceof String callbackQueryId)
				|| !(callback.get("data") instanceof String callbackData)
				|| !(callback.get("from") instanceof Map<?, ?> from)
				|| !(from.get("id") instanceof Number userId)
				|| !(callback.get("message") instanceof Map<?, ?> message)
				|| !(message.get("chat") instanceof Map<?, ?> chat)
				|| !(chat.get("id") instanceof Number chatId)) {
			return Optional.empty();
		}

		return Optional.of(TelegramUpdate.callback(
				updateId,
				userId.longValue(),
				chatId.longValue(),
				callbackQueryId,
				callbackData));
	}
}
