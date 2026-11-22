package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class CookingHistoryRestAcceptanceTest extends AbstractIntegrationTest {

	@Test
	void apiRecordsListsAndDeletesCookingHistory() throws IOException, InterruptedException {
		var recipe = recipeCatalogService.getCatalog().getFirst();
		Long recordId = null;

		try {
			var createResponse = sendJson(
					"POST",
					"/api/recipes/" + recipe.id() + "/cookings",
					"""
							{"cookedOn":"2026-11-18","rating":4,"note":"Добавить меньше соли"}
							""");

			assertThat(createResponse.statusCode()).isEqualTo(201);
			assertThat(createResponse.body()).contains("\"id\":");

			recordId = cookingHistoryService.getHistory().stream()
					.filter(item -> "Добавить меньше соли".equals(item.note()))
					.map(item -> item.id())
					.findFirst()
					.orElseThrow();
			var createdRecordId = recordId;

			var historyResponse = get("/api/cooking-history");

			assertThat(historyResponse.statusCode()).isEqualTo(200);
			assertThat(historyResponse.headers().firstValue("Content-Type").orElse(""))
					.startsWith("application/json");
			assertThat(historyResponse.body())
					.contains("\"total\":1")
					.contains("\"recipeName\":\"" + recipe.nameRu() + "\"")
					.contains("\"cookedOn\":\"2026-11-18\"")
					.contains("\"rating\":4")
					.contains("\"note\":\"Добавить меньше соли\"")
					.contains("\"archived\":false");

			var deleteResponse = sendJson(
					"DELETE",
					"/api/cooking-history/" + createdRecordId,
					null);

			assertThat(deleteResponse.statusCode()).isEqualTo(204);
			assertThat(cookingHistoryService.getHistory())
					.noneMatch(item -> item.id().equals(createdRecordId));
			recordId = null;
		} finally {
			if (recordId != null) {
				cookingHistoryService.delete(recordId);
			}
		}
	}

	@Test
	void apiRejectsInvalidCookingRecord() throws IOException, InterruptedException {
		var recipeId = recipeCatalogService.getCatalog().getFirst().id();
		var futureDate = LocalDate.now().plusDays(1);
		var longNote = "x".repeat(2001);
		var response = sendJson(
				"POST",
				"/api/recipes/" + recipeId + "/cookings",
				"{\"cookedOn\":\"" + futureDate + "\",\"rating\":6,\"note\":\""
						+ longNote + "\"}");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body())
				.contains("\"fieldErrors\":")
				.contains("\"cookedOn\":\"Дата приготовления не может быть в будущем\"")
				.contains("\"rating\":\"Оценка должна быть от 1 до 5\"")
				.contains("\"note\":\"Заметка должна быть не длиннее 2000 символов\"");
	}

	private HttpResponse<String> sendJson(String method, String path, String body)
			throws IOException, InterruptedException {
		var publisher = body == null
				? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.header("Content-Type", "application/json")
				.method(method, publisher)
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}
}
