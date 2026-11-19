package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeCatalogRestAcceptanceTest extends AbstractIntegrationTest {

	@Test
	void apiReturnsRecipeCatalogForFrontend() throws IOException, InterruptedException {
		var response = get("/api/recipes");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type").orElse(""))
				.startsWith("application/json");
		assertThat(response.body())
				.contains("\"items\":")
				.contains("\"total\":15")
				.contains("\"name\":\"Гречка с тушёной курицей\"")
				.contains("\"englishName\":\"Buckwheat with stewed chicken\"")
				.contains("\"coverUrl\":")
				.contains("\"totalMinutes\":")
				.contains("\"favorite\":false")
				.contains("\"restrictionWarnings\":[]");
	}

	@Test
	void apiFiltersCatalogForFrontend() throws IOException, InterruptedException {
		var response = get("/api/recipes?query=BUCKWHEAT&maxMinutes=50&difficulty=EASY");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("\"total\":1")
				.contains("\"name\":\"Гречка с тушёной курицей\"")
				.doesNotContain("Белорусские драники");
	}

	@Test
	void apiReturnsAvailableCatalogFilters() throws IOException, InterruptedException {
		var response = get("/api/recipes/filter-options");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type").orElse(""))
				.startsWith("application/json");
		assertThat(response.body())
				.contains("\"difficulties\"")
				.contains("\"value\":\"EASY\"")
				.contains("\"name\":\"Легко\"")
				.contains("\"equipment\"")
				.contains("Электрическая плита")
				.contains("\"cookingMethods\"")
				.contains("\"cuisines\"")
				.contains("\"origins\"")
				.contains("\"value\":\"CLASSIC\"");
	}
}
