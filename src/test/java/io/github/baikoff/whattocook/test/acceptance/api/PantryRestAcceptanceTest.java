package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class PantryRestAcceptanceTest extends AbstractIntegrationTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void apiReturnsAndReplacesPantrySettings() throws IOException, InterruptedException {
		var waterId = ingredientIds("Вода").getFirst();
		var oilId = ingredientIds("Растительное масло").getFirst();
		var saltId = ingredientIds("Соль").getFirst();

		try {
			var initialResponse = get("/api/pantry");
			var saveResponse = putJson(
					"/api/pantry",
					"{\"ingredientIds\":[" + waterId + "," + saltId + "]}");
			var updatedResponse = get("/api/pantry");

			assertThat(initialResponse.statusCode()).isEqualTo(200);
			assertThat(initialResponse.headers().firstValue("Content-Type").orElse(""))
					.startsWith("application/json");
			var initialSettings = objectMapper.readTree(initialResponse.body());
			assertThat(initialSettings.path("ingredients").toString())
					.contains("Вода", "Растительное масло", "Соль", "selected");

			assertThat(saveResponse.statusCode()).isEqualTo(200);
			var savedSettings = objectMapper.readTree(saveResponse.body());
			assertThat(savedSettings.path("selectedIngredientNames").toString())
					.isEqualTo("[\"Вода\",\"Соль\"]");

			assertThat(updatedResponse.statusCode()).isEqualTo(200);
			var updatedSettings = objectMapper.readTree(updatedResponse.body());
			assertThat(updatedSettings.path("selectedIngredientNames").toString())
					.isEqualTo("[\"Вода\",\"Соль\"]");
			assertThat(updatedSettings.path("ingredients").toString())
					.contains("{\"id\":" + oilId + ",\"name\":\"Растительное масло\",\"selected\":false}");
		} finally {
			postForm(
					"/preferences/pantry",
					java.util.stream.Stream.of(waterId, oilId, saltId)
							.map(id -> "ingredientIds=" + id)
							.collect(Collectors.joining("&")));
		}
	}

	private HttpResponse<String> putJson(String path, String body)
			throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.header("Content-Type", "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(body))
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}
}
