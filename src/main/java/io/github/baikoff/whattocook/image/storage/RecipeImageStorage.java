package io.github.baikoff.whattocook.image.storage;

import io.github.baikoff.whattocook.recipe.domain.Recipe;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Component
public class RecipeImageStorage {

	private static final String BUNDLED_PATH_PREFIX = "bundled:";
	private static final String BUNDLED_PUBLIC_URL_PREFIX = "/images/";
	private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
	private static final int MAX_IMAGE_SIDE = 8000;

	private final Path rootDirectory;

	public RecipeImageStorage(@Value("${app.images.directory:data/images}") String directory) {
		this.rootDirectory = Path.of(directory).toAbsolutePath().normalize();
	}

	public Optional<String> validationError(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return Optional.empty();
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			return Optional.of("Изображение должно быть не больше 5 МБ");
		}

		try {
			var bytes = file.getBytes();
			if (detectExtension(bytes).isEmpty()) {
				return Optional.of("Разрешены только изображения JPEG и PNG");
			}

			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (image == null) {
				return Optional.of("Файл не удалось прочитать как изображение");
			}
			if (image.getWidth() > MAX_IMAGE_SIDE || image.getHeight() > MAX_IMAGE_SIDE) {
				return Optional.of("Сторона изображения должна быть не больше 8000 пикселей");
			}
			return Optional.empty();
		} catch (IOException exception) {
			return Optional.of("Файл изображения не удалось прочитать");
		}
	}

	public String store(long recipeId, MultipartFile file) {
		var validationError = validationError(file);
		if (validationError.isPresent()) {
			throw new IllegalArgumentException(validationError.get());
		}

		try {
			var bytes = file.getBytes();
			var extension = detectExtension(bytes).orElseThrow();
			var recipeDirectory = rootDirectory.resolve("recipes").resolve(Long.toString(recipeId)).normalize();
			ensureInsideRoot(recipeDirectory);
			Files.createDirectories(recipeDirectory);

			var fileName = UUID.randomUUID() + "." + extension;
			var target = recipeDirectory.resolve(fileName);
			var temporary = recipeDirectory.resolve(fileName + ".tmp");
			Files.write(temporary, bytes);

			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, target);
			}

			deleteOnRollback(target);
			return rootDirectory.relativize(target).toString().replace('\\', '/');
		} catch (IOException exception) {
			throw new ImageStorageException("Не удалось сохранить изображение", exception);
		}
	}

	public void deleteAfterCommit(String relativePath) {
		if (relativePath == null || relativePath.isBlank() || isBundled(relativePath)) {
			return;
		}

		var target = resolveSafely(relativePath);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					deleteQuietly(target);
				}
			});
		} else {
			deleteQuietly(target);
		}
	}

	public Path getRootDirectory() {
		return rootDirectory;
	}

	public String publicUrl(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return null;
		}

		var normalizedPath = relativePath.replace('\\', '/');
		if (isBundled(normalizedPath)) {
			return BUNDLED_PUBLIC_URL_PREFIX + normalizedPath.substring(BUNDLED_PATH_PREFIX.length());
		}
		return "/user-images/" + normalizedPath;
	}

	private boolean isBundled(String relativePath) {
		return relativePath.startsWith(BUNDLED_PATH_PREFIX);
	}

	private Optional<String> detectExtension(byte[] bytes) {
		if (bytes.length >= 8
				&& (bytes[0] & 0xff) == 0x89
				&& bytes[1] == 0x50
				&& bytes[2] == 0x4e
				&& bytes[3] == 0x47
				&& bytes[4] == 0x0d
				&& bytes[5] == 0x0a
				&& bytes[6] == 0x1a
				&& bytes[7] == 0x0a) {
			return Optional.of("png");
		}
		if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8) {
			return Optional.of("jpg");
		}
		return Optional.empty();
	}

	private void deleteOnRollback(Path target) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != TransactionSynchronization.STATUS_COMMITTED) {
					deleteQuietly(target);
				}
			}
		});
	}

	private Path resolveSafely(String relativePath) {
		var target = rootDirectory.resolve(relativePath).normalize();
		ensureInsideRoot(target);
		return target;
	}

	private void ensureInsideRoot(Path path) {
		if (!path.startsWith(rootDirectory)) {
			throw new IllegalArgumentException("Недопустимый путь изображения");
		}
	}

	private void deleteQuietly(Path target) {
		try {
			Files.deleteIfExists(target);
			var parent = target.getParent();
			if (parent != null && !parent.equals(rootDirectory)) {
				try {
					Files.delete(parent);
				} catch (IOException ignored) {
					// The directory still contains other recipe images.
				}
			}
		} catch (IOException ignored) {
			// A missing or locked old image must not roll back recipe changes.
		}
	}
}
