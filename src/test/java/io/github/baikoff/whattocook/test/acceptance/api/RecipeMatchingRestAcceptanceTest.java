package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeMatchingRestAcceptanceTest extends AbstractIntegrationTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void apiReturnsIngredientsAndMatchesRecipesForSelectedProduct() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();

		var emptyResponse = get("/api/recipe-matches");
		var matchingResponse = get("/api/recipe-matches?ingredientIds=" + paprikaId);

		assertThat(emptyResponse.statusCode()).isEqualTo(200);
		assertThat(emptyResponse.headers().firstValue("Content-Type").orElse(""))
				.startsWith("application/json");
		var emptyPage = objectMapper.readTree(emptyResponse.body());
		assertThat(emptyPage.path("ingredients").toString()).contains("Паприка", "Картофель");
		assertThat(emptyPage.path("selectedIngredientIds")).isEmpty();
		assertThat(emptyPage.path("recipes")).isEmpty();

		assertThat(matchingResponse.statusCode()).isEqualTo(200);
		var matchingPage = objectMapper.readTree(matchingResponse.body());
		assertThat(matchingPage.path("selectedIngredientIds").toString())
				.contains(String.valueOf(paprikaId));
		assertThat(matchingPage.path("recipes")).hasSize(2);
		var firstRecipe = matchingPage.path("recipes").get(0);
		assertThat(firstRecipe.path("nameRu").asText())
				.isEqualTo("Картофель, запечённый в духовке");
		assertThat(firstRecipe.path("id").isNumber()).isTrue();
		assertThat(firstRecipe.path("description").asText()).isNotBlank();
		assertThat(firstRecipe.has("coverUrl")).isTrue();
		assertThat(firstRecipe.path("matchPercentage").asInt()).isPositive();
		assertThat(firstRecipe.path("totalMinutes").asInt()).isPositive();
		assertThat(firstRecipe.path("difficulty").asText()).isNotBlank();
	}
}
