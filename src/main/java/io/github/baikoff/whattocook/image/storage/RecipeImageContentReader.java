package io.github.baikoff.whattocook.image.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class RecipeImageContentReader {

	private static final String BUNDLED_URL_PREFIX = "/images/";
	private static final String USER_URL_PREFIX = "/user-images/";

	private final RecipeImageStorage imageStorage;
	private final ResourceLoader resourceLoader;

	public RecipeImageContentReader(
			RecipeImageStorage imageStorage,
			ResourceLoader resourceLoader) {
		this.imageStorage = imageStorage;
		this.resourceLoader = resourceLoader;
	}

	public Optional<RecipeImageContent> read(String publicUrl) {
		if (publicUrl == null || publicUrl.isBlank()) {
			return Optional.empty();
		}
		try {
			if (publicUrl.startsWith(BUNDLED_URL_PREFIX)) {
				return readBundled(publicUrl.substring(BUNDLED_URL_PREFIX.length()));
			}
			if (publicUrl.startsWith(USER_URL_PREFIX)) {
				return readUserImage(publicUrl.substring(USER_URL_PREFIX.length()));
			}
			return Optional.empty();
		} catch (IOException exception) {
			return Optional.empty();
		}
	}

	private Optional<RecipeImageContent> readBundled(String relativeValue) throws IOException {
		var relativePath = safeRelativePath(relativeValue);
		if (relativePath.isEmpty()) {
			return Optional.empty();
		}
		var resourcePath = relativePath.get().toString().replace('\\', '/');
		var resource = resourceLoader.getResource("classpath:/static/images/" + resourcePath);
		if (!resource.exists() || !resource.isReadable()) {
			return Optional.empty();
		}
		try (var input = resource.getInputStream()) {
			return content(resource.getFilename(), input.readAllBytes());
		}
	}

	private Optional<RecipeImageContent> readUserImage(String relativeValue) throws IOException {
		var relativePath = safeRelativePath(relativeValue);
		if (relativePath.isEmpty()) {
			return Optional.empty();
		}
		var root = imageStorage.getRootDirectory();
		var target = root.resolve(relativePath.get()).normalize();
		if (!target.startsWith(root) || !Files.isRegularFile(target)) {
			return Optional.empty();
		}
		return content(target.getFileName().toString(), Files.readAllBytes(target));
	}

	private Optional<Path> safeRelativePath(String value) {
		if (value.isBlank() || value.indexOf('\\') >= 0) {
			return Optional.empty();
		}
		try {
			var path = Path.of(value).normalize();
			if (path.isAbsolute() || path.startsWith("..")) {
				return Optional.empty();
			}
			return Optional.of(path);
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	private Optional<RecipeImageContent> content(String fileName, byte[] bytes) {
		if (fileName == null) {
			return Optional.empty();
		}
		var extension = extension(fileName);
		var mediaType = switch (extension) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			default -> null;
		};
		return mediaType == null
				? Optional.empty()
				: Optional.of(new RecipeImageContent(fileName, mediaType, bytes));
	}

	private String extension(String fileName) {
		var dotIndex = fileName.lastIndexOf('.');
		return dotIndex < 0
				? ""
				: fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
	}
}
