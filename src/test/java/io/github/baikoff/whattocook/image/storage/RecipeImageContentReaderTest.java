package io.github.baikoff.whattocook.image.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

class RecipeImageContentReaderTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void readsBundledImageFromClasspath() {
		var reader = reader();

		var image = reader.read("/images/recipes/classic/classic-syrniki.jpg");

		assertThat(image).hasValueSatisfying(content -> {
			assertThat(content.fileName()).isEqualTo("classic-syrniki.jpg");
			assertThat(content.mediaType()).isEqualTo("image/jpeg");
			assertThat(content.bytes()).isNotEmpty();
		});
	}

	@Test
	void readsUserImageOnlyInsideStorageRoot() throws IOException {
		var imagePath = temporaryDirectory.resolve("recipes/15/user-cover.png");
		Files.createDirectories(imagePath.getParent());
		Files.write(imagePath, new byte[] {1, 2, 3});
		var reader = reader();

		assertThat(reader.read("/user-images/recipes/15/user-cover.png"))
				.hasValueSatisfying(content -> {
					assertThat(content.fileName()).isEqualTo("user-cover.png");
					assertThat(content.mediaType()).isEqualTo("image/png");
					assertThat(content.bytes()).containsExactly(1, 2, 3);
				});
		assertThat(reader.read("/user-images/../outside.png")).isEmpty();
	}

	@Test
	void ignoresMissingAndUnsupportedImages() {
		var reader = reader();

		assertThat(reader.read(null)).isEmpty();
		assertThat(reader.read("/images/missing.jpg")).isEmpty();
		assertThat(reader.read("https://example.com/dish.jpg")).isEmpty();
		assertThat(reader.read("/images/recipes/notes.txt")).isEmpty();
	}

	private RecipeImageContentReader reader() {
		return new RecipeImageContentReader(
				new RecipeImageStorage(temporaryDirectory.toString()),
				new DefaultResourceLoader());
	}
}
