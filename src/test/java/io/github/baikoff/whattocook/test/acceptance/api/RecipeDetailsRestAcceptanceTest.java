package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeDetailsRestAcceptanceTest extends AbstractIntegrationTest {

	@Test
	void apiReturnsRecipeDetailsAndRecalculatesIngredientsForRequestedServings()
			throws IOException, InterruptedException {
		var recipe = recipeCatalogService.getCatalog().stream()
				.filter(item -> item.nameRu().equals("Гречка с тушёной курицей"))
				.findFirst()
				.orElseThrow();

		var response = get("/api/recipes/" + recipe.id() + "?servings=4");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type").orElse(""))
				.startsWith("application/json");
		assertThat(response.body())
				.contains("\"name\":\"Гречка с тушёной курицей\"")
				.contains("\"baseServings\":2")
				.contains("\"servings\":4")
				.contains("\"name\":\"Куриное филе\",\"amount\":\"600\",\"unit\":\"г\"")
				.contains("\"instruction\":\"Промыть гречку.")
				.contains("\"equipment\":[")
				.contains("\"cookingMethods\":[");
	}
}
