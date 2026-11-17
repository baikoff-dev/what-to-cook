package io.github.baikoff.whattocook.telegram.config;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.telegram.client.TelegramBotApiClient;
import io.github.baikoff.whattocook.telegram.polling.TelegramLongPollingRunner;
import io.github.baikoff.whattocook.telegram.service.TelegramRecommendationService;
import io.github.baikoff.whattocook.telegram.service.TelegramUpdateHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.telegram", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfiguration {

	@Bean
	TelegramBotApiClient telegramBotApiClient(TelegramProperties properties) {
		return new TelegramBotApiClient(properties);
	}

	@Bean
	TelegramRecommendationService telegramRecommendationService(
			DishRecommendationService recommendationService) {
		return new TelegramRecommendationService(recommendationService);
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
