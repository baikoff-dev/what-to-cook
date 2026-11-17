package io.github.baikoff.whattocook.telegram.model;

public record TelegramUpdate(
		long updateId,
		long userId,
		long chatId,
		String text,
		String callbackQueryId,
		String callbackData) {

	public TelegramUpdate(long updateId, long userId, long chatId, String text) {
		this(updateId, userId, chatId, text, null, null);
	}

	public static TelegramUpdate callback(
			long updateId,
			long userId,
			long chatId,
			String callbackQueryId,
			String callbackData) {
		return new TelegramUpdate(
				updateId,
				userId,
				chatId,
				null,
				callbackQueryId,
				callbackData);
	}

	public boolean hasCallbackQuery() {
		return callbackQueryId != null && !callbackQueryId.isBlank();
	}
}
