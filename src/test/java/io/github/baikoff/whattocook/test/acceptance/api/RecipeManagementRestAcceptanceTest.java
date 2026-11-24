package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
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

	@Test
	void apiReplacesRecipeCoverFromMultipartReactForm()
			throws IOException, InterruptedException {
		var equipmentId = recipeManagementService.getEquipmentOptions().getFirst().id();
		var cookingMethodId = recipeManagementService.getCookingMethodOptions().getFirst().id();
		var cuisineId = recipeManagementService.getCuisineOptions().getFirst().id();
		Long recipeId = null;

		try {
			var createResponse = sendJson(
					"POST",
					"/api/recipes",
					createRequest(equipmentId, cookingMethodId, cuisineId));
			recipeId = objectMapper.readTree(createResponse.body()).path("id").longValue();

			var editRecipe = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			var updateResponse = sendMultipart(
					"PUT",
					"/api/recipes/" + recipeId,
					updateRequest(
							editRecipe.path("version").longValue(),
							editRecipe.path("steps").get(0).path("id").longValue(),
							equipmentId,
							cookingMethodId,
							cuisineId),
					validPng().getBytes());

			assertThat(updateResponse.statusCode()).isEqualTo(200);
			var updatedRecipe = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			assertThat(updatedRecipe.path("mainImageUrl").textValue())
					.startsWith("/user-images/recipes/" + recipeId + "/");
		} finally {
			if (recipeId != null && recipeRepository.existsById(recipeId)) {
				recipeRepository.deleteById(recipeId);
			}
		}
	}

	@Test
	void apiRemovesRecipeCoverOnlyAfterReactFormIsSaved()
			throws IOException, InterruptedException {
		var equipmentId = recipeManagementService.getEquipmentOptions().getFirst().id();
		var cookingMethodId = recipeManagementService.getCookingMethodOptions().getFirst().id();
		var cuisineId = recipeManagementService.getCuisineOptions().getFirst().id();
		Long recipeId = null;

		try {
			var createResponse = sendJson(
					"POST",
					"/api/recipes",
					createRequest(equipmentId, cookingMethodId, cuisineId));
			recipeId = objectMapper.readTree(createResponse.body()).path("id").longValue();

			var editRecipe = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			var uploadResponse = sendMultipart(
					"PUT",
					"/api/recipes/" + recipeId,
					updateRequest(
							editRecipe.path("version").longValue(),
							editRecipe.path("steps").get(0).path("id").longValue(),
							equipmentId,
							cookingMethodId,
							cuisineId),
					validPng().getBytes());
			assertThat(uploadResponse.statusCode()).isEqualTo(200);

			var recipeWithCover = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			assertThat(recipeWithCover.path("mainImageUrl").textValue()).isNotBlank();

			var removeResponse = sendJson(
					"PUT",
					"/api/recipes/" + recipeId,
					updateRequest(
							recipeWithCover.path("version").longValue(),
							recipeWithCover.path("steps").get(0).path("id").longValue(),
							equipmentId,
							cookingMethodId,
							cuisineId,
							true));

			assertThat(removeResponse.statusCode()).isEqualTo(200);
			var recipeWithoutCover = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			assertThat(recipeWithoutCover.path("mainImageUrl").isNull()).isTrue();
		} finally {
			if (recipeId != null && recipeRepository.existsById(recipeId)) {
				recipeRepository.deleteById(recipeId);
			}
		}
	}

	@Test
	void apiUploadsImageForSpecificRecipeStepFromMultipartReactForm()
			throws IOException, InterruptedException {
		var equipmentId = recipeManagementService.getEquipmentOptions().getFirst().id();
		var cookingMethodId = recipeManagementService.getCookingMethodOptions().getFirst().id();
		var cuisineId = recipeManagementService.getCuisineOptions().getFirst().id();
		Long recipeId = null;

		try {
			var createResponse = sendJson(
					"POST",
					"/api/recipes",
					createRequest(equipmentId, cookingMethodId, cuisineId));
			recipeId = objectMapper.readTree(createResponse.body()).path("id").longValue();

			var editRecipe = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			var updateResponse = sendStepImageMultipart(
					"PUT",
					"/api/recipes/" + recipeId,
					updateRequest(
							editRecipe.path("version").longValue(),
							editRecipe.path("steps").get(0).path("id").longValue(),
							equipmentId,
							cookingMethodId,
							cuisineId),
					validPng().getBytes());

			assertThat(updateResponse.statusCode()).isEqualTo(200);
			var updatedRecipe = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			assertThat(updatedRecipe.path("steps").get(0).path("imageUrl").textValue())
					.startsWith("/user-images/recipes/" + recipeId + "/");
		} finally {
			if (recipeId != null && recipeRepository.existsById(recipeId)) {
				recipeRepository.deleteById(recipeId);
			}
		}
	}

	@Test
	void apiRemovesStepImageOnlyAfterReactFormIsSaved()
			throws IOException, InterruptedException {
		var equipmentId = recipeManagementService.getEquipmentOptions().getFirst().id();
		var cookingMethodId = recipeManagementService.getCookingMethodOptions().getFirst().id();
		var cuisineId = recipeManagementService.getCuisineOptions().getFirst().id();
		Long recipeId = null;

		try {
			var createResponse = sendJson(
					"POST",
					"/api/recipes",
					createRequest(equipmentId, cookingMethodId, cuisineId));
			recipeId = objectMapper.readTree(createResponse.body()).path("id").longValue();

			var editRecipe = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			var uploadResponse = sendStepImageMultipart(
					"PUT",
					"/api/recipes/" + recipeId,
					updateRequest(
							editRecipe.path("version").longValue(),
							editRecipe.path("steps").get(0).path("id").longValue(),
							equipmentId,
							cookingMethodId,
							cuisineId),
					validPng().getBytes());
			assertThat(uploadResponse.statusCode()).isEqualTo(200);

			var recipeWithStepImage = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			assertThat(recipeWithStepImage.path("steps").get(0).path("imageUrl").textValue()).isNotBlank();

			var removeResponse = sendJson(
					"PUT",
					"/api/recipes/" + recipeId,
					updateRequestRemovingStepImage(
							recipeWithStepImage.path("version").longValue(),
							recipeWithStepImage.path("steps").get(0).path("id").longValue(),
							equipmentId,
							cookingMethodId,
							cuisineId));

			assertThat(removeResponse.statusCode()).isEqualTo(200);
			var recipeWithoutStepImage = objectMapper.readTree(get("/api/recipes/" + recipeId + "/edit").body());
			assertThat(recipeWithoutStepImage.path("steps").get(0).path("imageUrl").isNull()).isTrue();
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

	private String updateRequest(
			long version,
			long stepId,
			long equipmentId,
			long cookingMethodId,
			long cuisineId,
			boolean removeMainImage) {
		var request = updateRequest(
				version,
				stepId,
				equipmentId,
				cookingMethodId,
				cuisineId);
		var openingBrace = request.indexOf('{');
		return request.substring(0, openingBrace + 1)
				+ "\n  \"removeMainImage\": " + removeMainImage + ","
				+ request.substring(openingBrace + 1);
	}

	private String updateRequestRemovingStepImage(
			long version,
			long stepId,
			long equipmentId,
			long cookingMethodId,
			long cuisineId) {
		return updateRequest(version, stepId, equipmentId, cookingMethodId, cuisineId)
				.replace(
						"\"instruction\": \"Промыть рис и сварить до готовности.\"",
						"\"instruction\": \"Промыть рис и сварить до готовности.\", \"removeImage\": true");
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

	private HttpResponse<String> sendMultipart(
			String method,
			String path,
			String recipeJson,
			byte[] mainImage) throws IOException, InterruptedException {
		var boundary = "----WhatToCookRecipeManagementBoundary";
		var body = new ByteArrayOutputStream();
		body.write(("--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"recipe\"\r\n"
				+ "Content-Type: application/json; charset=UTF-8\r\n\r\n")
				.getBytes(StandardCharsets.UTF_8));
		body.write(recipeJson.getBytes(StandardCharsets.UTF_8));
		body.write(("\r\n--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"mainImage\"; filename=\"cover.png\"\r\n"
				+ "Content-Type: image/png\r\n\r\n")
				.getBytes(StandardCharsets.UTF_8));
		body.write(mainImage);
		body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.method(method, HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> sendStepImageMultipart(
			String method,
			String path,
			String recipeJson,
			byte[] stepImage) throws IOException, InterruptedException {
		var boundary = "----WhatToCookStepImageBoundary";
		var body = new ByteArrayOutputStream();
		body.write(("--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"recipe\"\r\n"
				+ "Content-Type: application/json; charset=UTF-8\r\n\r\n")
				.getBytes(StandardCharsets.UTF_8));
		body.write(recipeJson.getBytes(StandardCharsets.UTF_8));
		body.write(("\r\n--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"stepImageIndexes\"\r\n"
				+ "Content-Type: application/json\r\n\r\n"
				+ "[0]")
				.getBytes(StandardCharsets.UTF_8));
		body.write(("\r\n--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"stepImages\"; filename=\"step-1.png\"\r\n"
				+ "Content-Type: image/png\r\n\r\n")
				.getBytes(StandardCharsets.UTF_8));
		body.write(stepImage);
		body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.method(method, HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}
}
