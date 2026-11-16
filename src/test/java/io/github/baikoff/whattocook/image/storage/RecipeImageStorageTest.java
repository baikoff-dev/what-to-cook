package io.github.baikoff.whattocook.image.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecipeImageStorageTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void resolvesBundledAndUserImageUrls() {
		var storage = new RecipeImageStorage(temporaryDirectory.toString());

		assertThat(storage.publicUrl("bundled:recipes/classic/shakshuka.png"))
				.isEqualTo("/images/recipes/classic/shakshuka.png");
		assertThat(storage.publicUrl("recipes/15/user-cover.png"))
				.isEqualTo("/user-images/recipes/15/user-cover.png");
	}

	@Test
	void ignoresBundledImageWhenDeletingUserFile() {
		var storage = new RecipeImageStorage(temporaryDirectory.toString());

		assertThatCode(() -> storage.deleteAfterCommit("bundled:recipes/classic/shakshuka.png"))
				.doesNotThrowAnyException();
	}
}
