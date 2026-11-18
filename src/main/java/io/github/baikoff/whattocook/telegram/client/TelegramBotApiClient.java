package io.github.baikoff.whattocook.telegram.client;

import io.github.baikoff.whattocook.telegram.config.TelegramProperties;
import io.github.baikoff.whattocook.telegram.model.TelegramOutgoingMessage;
import io.github.baikoff.whattocook.telegram.model.TelegramUpdate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class TelegramBotApiClient {

	private static final int LONG_POLLING_TIMEOUT_SECONDS = 25;
	private static final int MAX_PHOTO_CAPTION_LENGTH = 1024;

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public TelegramBotApiClient(
			TelegramProperties properties,
			ObjectMapper objectMapper) {
		var apiBaseUrl = properties.getApiBaseUrl().replaceAll("/+$", "");
		this.restClient = RestClient.builder()
				.baseUrl(apiBaseUrl + "/bot" + properties.getToken())
				.build();
		this.objectMapper = objectMapper;
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
		if (message.hasPhoto()) {
			sendPhoto(message);
			return;
		}
		sendTextMessage(message);
	}

	private void sendTextMessage(TelegramOutgoingMessage message) {
		var request = new LinkedHashMap<String, Object>();
		request.put("chat_id", message.chatId());
		request.put("text", message.text());
		request.put("reply_markup", replyMarkup(message));

		restClient.post()
				.uri("/sendMessage")
				.body(request)
				.retrieve()
				.toBodilessEntity();
	}

	private void sendPhoto(TelegramOutgoingMessage message) {
		var photo = message.photo();
		var parts = new LinkedMultiValueMap<String, Object>();
		parts.add("chat_id", Long.toString(message.chatId()));
		parts.add("caption", photoCaption(message.text()));
		parts.add("reply_markup", json(replyMarkup(message)));

		var photoHeaders = new HttpHeaders();
		photoHeaders.setContentType(MediaType.parseMediaType(photo.mediaType()));
		parts.add("photo", new HttpEntity<>(namedResource(photo.bytes(), photo.fileName()), photoHeaders));

		restClient.post()
				.uri("/sendPhoto")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(parts)
				.retrieve()
				.toBodilessEntity();
	}

	private Map<String, Object> replyMarkup(TelegramOutgoingMessage message) {
		var buttons = message.buttons().stream()
				.map(button -> Map.of(
						"text", button.text(),
						"callback_data", button.callbackData()))
				.toList();
		return Map.of("inline_keyboard", List.of(buttons));
	}

	private String json(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Не удалось сформировать Telegram-клавиатуру", exception);
		}
	}

	private String photoCaption(String text) {
		if (text.length() <= MAX_PHOTO_CAPTION_LENGTH) {
			return text;
		}
		var lastAllowedIndex = MAX_PHOTO_CAPTION_LENGTH - 1;
		var boundary = text.lastIndexOf('\n', lastAllowedIndex);
		if (boundary < MAX_PHOTO_CAPTION_LENGTH / 2) {
			boundary = text.lastIndexOf(' ', lastAllowedIndex);
		}
		if (boundary <= 0) {
			boundary = lastAllowedIndex;
		}
		return text.substring(0, boundary).stripTrailing() + "…";
	}

	private ByteArrayResource namedResource(byte[] bytes, String fileName) {
		return new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return fileName;
			}
		};
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
