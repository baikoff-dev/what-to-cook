package io.github.baikoff.whattocook.image.storage;

public record RecipeImageContent(
		String fileName,
		String mediaType,
		byte[] bytes) {

	public RecipeImageContent {
		bytes = bytes.clone();
	}

	@Override
	public byte[] bytes() {
		return bytes.clone();
	}
}
