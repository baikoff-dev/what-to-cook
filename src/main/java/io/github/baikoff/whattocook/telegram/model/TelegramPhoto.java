package io.github.baikoff.whattocook.telegram.model;

public record TelegramPhoto(
		String fileName,
		String mediaType,
		byte[] bytes) {

	public TelegramPhoto {
		bytes = bytes.clone();
	}

	@Override
	public byte[] bytes() {
		return bytes.clone();
	}
}
