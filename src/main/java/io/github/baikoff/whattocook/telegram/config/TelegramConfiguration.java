package io.github.baikoff.whattocook.telegram.config;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.image.storage.RecipeImageContentReader;
import io.github.baikoff.whattocook.telegram.client.TelegramBotApiClient;
import io.github.baikoff.whattocook.telegram.polling.TelegramLongPollingRunner;
import io.github.baikoff.whattocook.telegram.service.TelegramRecommendationService;
import io.github.baikoff.whattocook.telegram.service.TelegramUpdateHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.telegram", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfiguration {

	@Bean
	TelegramBotApiClient telegramBotApiClient(
			TelegramProperties properties,
			ObjectMapper objectMapper) {
		return new TelegramBotApiClient(properties, objectMapper);
	}

	@Bean
	TelegramRecommendationService telegramRecommendationService(
			DishRecommendationService recommendationService,
			RecipeImageContentReader imageContentReader) {
		return new TelegramRecommendationService(recommendationService, imageContentReader);
	}

	@Bean
	TelegramUpdateHandler telegramUpdateHandler(
			TelegramProperties properties,
			TelegramRecommendationService recommendationService) {
		return new TelegramUpdateHandler(
				properties.getAllowedUserIds(),
				recommendationService);
	}

	@Bean
	TelegramLongPollingRunner telegramLongPollingRunner(
			TelegramBotApiClient client,
			TelegramUpdateHandler updateHandler) {
		return new TelegramLongPollingRunner(client, updateHandler);
	}
}
