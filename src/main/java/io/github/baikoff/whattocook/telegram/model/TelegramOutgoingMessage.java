package io.github.baikoff.whattocook.telegram.model;

import java.util.List;

public record TelegramOutgoingMessage(
		long chatId,
		String text,
		List<TelegramButton> buttons,
		TelegramPhoto photo) {

	public TelegramOutgoingMessage(
			long chatId,
			String text,
			List<TelegramButton> buttons) {
		this(chatId, text, buttons, null);
	}

	public TelegramOutgoingMessage {
		buttons = List.copyOf(buttons);
	}

	public boolean hasPhoto() {
		return photo != null;
	}
}
