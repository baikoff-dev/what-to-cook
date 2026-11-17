package io.github.baikoff.whattocook.test.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

@Import(SurpriseRecommendationAcceptanceTest.DeterministicRandomConfiguration.class)
class SurpriseRecommendationAcceptanceTest extends AbstractIntegrationTest {

	private static final Pattern RECIPE_LINK = Pattern.compile("href=\"/recipes/(\\d+)\"");

	@Test
	@DisplayName("Повторный запрос «Не знаю, предложи сам» предлагает другой набор блюд")
	void repeatedSurpriseRequestsOfferDifferentRecipeSets() throws IOException, InterruptedException {
		var firstResponse = get("/assistant?mode=SURPRISE");
		var secondResponse = get("/assistant?mode=SURPRISE");

		assertThat(firstResponse.statusCode()).isEqualTo(200);
		assertThat(secondResponse.statusCode()).isEqualTo(200);

		var firstRecipeIds = extractRecipeIds(firstResponse.body());
		var secondRecipeIds = extractRecipeIds(secondResponse.body());

		assertThat(firstRecipeIds).hasSize(3);
		assertThat(secondRecipeIds).hasSize(3);
		assertThat(secondRecipeIds).isNotEqualTo(firstRecipeIds);
	}

	private Set<Long> extractRecipeIds(String responseBody) {
		var recipeIds = new LinkedHashSet<Long>();
		var matcher = RECIPE_LINK.matcher(responseBody);
		while (matcher.find()) {
			recipeIds.add(Long.parseLong(matcher.group(1)));
		}
		return recipeIds;
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class DeterministicRandomConfiguration {

		@Bean
		@Primary
		RandomGenerator deterministicRecommendationRandomGenerator() {
			return new Random(42L);
		}
	}
}
