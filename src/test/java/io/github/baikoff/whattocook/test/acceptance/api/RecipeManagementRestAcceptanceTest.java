package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;
import tools.jackson.databind.ObjectMapper;

class RecipeManagementRestAcceptanceTest extends AbstractIntegrationTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RecipeRepository recipeRepository;

	@Test
	void apiCreatesLoadsAndUpdatesCompleteRecipeForReactForm()
			throws IOException, InterruptedException {
		var equipmentId = recipeManagementService.getEquipmentOptions().getFirst().id();
		var cookingMethodId = recipeManagementService.getCookingMethodOptions().getFirst().id();
		var cuisineId = recipeManagementService.getCuisineOptions().getFirst().id();
		Long recipeId = null;

		try {
			var optionsResponse = get("/api/recipes/form-options");

			assertThat(optionsResponse.statusCode()).isEqualTo(200);
			assertThat(optionsResponse.body())
					.contains("\"statuses\"")
					.contains("\"measurementUnits\"")
					.contains("\"substitutionTypes\"")
					.contains("\"equipment\"")
					.contains("\"cookingMethods\"")
					.contains("\"cuisines\"");

			var createResponse = sendJson(
					"POST",
					"/api/recipes",
					createRequest(equipmentId, cookingMethodId, cuisineId));

			assertThat(createResponse.statusCode()).isEqualTo(201);
			recipeId = objectMapper.readTree(createResponse.body()).path("id").longValue();
			assertThat(recipeId).isPositive();

			var editResponse = get("/api/recipes/" + recipeId + "/edit");

			assertThat(editResponse.statusCode()).isEqualTo(200);
			var editRecipe = objectMapper.readTree(editResponse.body());
			assertThat(editRecipe.path("nameRu").textValue()).isEqualTo("Рис для acceptance-теста");
			assertThat(editRecipe.path("ingredients").get(0).path("name").textValue())
					.isEqualTo("Рис acceptance");
			assertThat(editRecipe.path("substitutions").get(0).path("substituteIngredientName").textValue())
					.isEqualTo("Булгур acceptance");
			assertThat(editRecipe.path("steps").get(0).path("id").longValue()).isPositive();

			var updateResponse = sendJson(
					"PUT",
					"/api/recipes/" + recipeId,
					updateRequest(
							editRecipe.path("version").longValue(),
							editRecipe.path("steps").get(0).path("id").longValue(),
							equipmentId,
							cookingMethodId,
							cuisineId));

			assertThat(updateResponse.statusCode()).isEqualTo(200);
			assertThat(updateResponse.body()).contains("\"id\":" + recipeId);

			var detailsResponse = get("/api/recipes/" + recipeId);

			assertThat(detailsResponse.statusCode()).isEqualTo(200);
			assertThat(detailsResponse.body())
					.contains("\"name\":\"Рис для acceptance-теста — изменён\"")
					.contains("\"name\":\"Рис acceptance\"")
					.contains("\"ingredientName\":\"Булгур acceptance\"")
					.contains("\"instruction\":\"Промыть рис и сварить до готовности.\"");
		} finally {
			if (recipeId != null && recipeRepository.existsById(recipeId)) {
				recipeRepository.deleteById(recipeId);
			}
		}
	}

	private String createRequest(long equipmentId, long cookingMethodId, long cuisineId) {
		return requestJson(
				0,
				"Рис для acceptance-теста",
				null,
				equipmentId,
				cookingMethodId,
				cuisineId);
	}

	private String updateRequest(
			long version,
			long stepId,
			long equipmentId,
			long cookingMethodId,
			long cuisineId) {
		return requestJson(
				version,
				"Рис для acceptance-теста — изменён",
				stepId,
				equipmentId,
				cookingMethodId,
				cuisineId);
	}

	private String requestJson(
			long version,
			String nameRu,
			Long stepId,
			long equipmentId,
			long cookingMethodId,
			long cuisineId) {
		return """
				{
				  "version": %d,
				  "nameRu": "%s",
				  "nameEn": "Acceptance rice",
				  "description": "Полный рецепт для проверки React-формы",
				  "servings": 2,
				  "preparationMinutes": 5,
				  "cookingMinutes": 20,
				  "difficulty": "EASY",
				  "status": "UNTESTED",
				  "cuisineId": %d,
				  "origin": "PERSONAL",
				  "sourceName": "Acceptance-тест",
				  "sourceUrl": "https://example.com/acceptance-rice",
				  "videoUrl": "https://example.com/acceptance-rice-video",
				  "personalNotes": "Проверить количество воды",
				  "equipmentIds": [%d],
				  "cookingMethodIds": [%d],
				  "ingredients": [
				    {"name": "Рис acceptance", "amount": 150, "unit": "GRAM", "optional": false}
				  ],
				  "substitutions": [
				    {
				      "originalIngredientName": "Рис acceptance",
				      "substituteIngredientName": "Булгур acceptance",
				      "amount": 150,
				      "unit": "GRAM",
				      "type": "CLOSE",
				      "impactNote": "Изменится текстура блюда"
				    }
				  ],
				  "steps": [
				    {"id": %s, "instruction": "Промыть рис и сварить до готовности."}
				  ]
				}
				""".formatted(
				version,
				nameRu,
				cuisineId,
				equipmentId,
				cookingMethodId,
				stepId == null ? "null" : stepId);
	}

	private HttpResponse<String> sendJson(String method, String path, String body)
			throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.header("Content-Type", "application/json")
				.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}
}
