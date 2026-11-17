package io.github.baikoff.whattocook.telegram.model;

import java.util.List;

public record TelegramOutgoingMessage(
		long chatId,
		String text,
		List<TelegramButton> buttons) {

	public TelegramOutgoingMessage {
		buttons = List.copyOf(buttons);
	}
}
