package io.github.baikoff.whattocook.image.configuration;

import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageWebConfiguration implements WebMvcConfigurer {

	private final RecipeImageStorage imageStorage;

	public ImageWebConfiguration(RecipeImageStorage imageStorage) {
		this.imageStorage = imageStorage;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		var location = imageStorage.getRootDirectory().toUri().toString();
		if (!location.endsWith("/")) {
			location += "/";
		}
		registry.addResourceHandler("/user-images/**")
				.addResourceLocations(location);
	}
}
