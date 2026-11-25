package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;
import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class IngredientRestrictionRestAcceptanceTest extends AbstractIntegrationTest {

	@Test
	void apiCreatesRestrictionRule() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();
		recipeRestrictionService.delete(paprikaId);

		try {
			var response = putJson(
					"/api/ingredient-restrictions/" + paprikaId,
					"""
							{"mode":"WARN","note":"  Можно редко  "}
							""");

			assertThat(response.statusCode()).isEqualTo(201);
			assertThat(response.headers().firstValue("Location"))
					.contains("/api/ingredient-restrictions/" + paprikaId);
			assertThat(recipeRestrictionService.getRules())
					.singleElement()
					.satisfies(rule -> {
						assertThat(rule.ingredientName()).isEqualTo("Паприка");
						assertThat(rule.mode()).isEqualTo(RestrictionMode.WARN);
						assertThat(rule.note()).isEqualTo("Можно редко");
					});
		} finally {
			recipeRestrictionService.delete(paprikaId);
		}
	}

	@Test
	void apiUpdatesRestrictionRule() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();
		recipeRestrictionService.delete(paprikaId);

		try {
			recipeRestrictionService.save(restrictionForm(
					paprikaId,
					RestrictionMode.WARN,
					"Можно редко"));

			var response = putJson(
					"/api/ingredient-restrictions/" + paprikaId,
					"""
							{"mode":"EXCLUDE","note":"Временно не использовать"}
							""");

			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(recipeRestrictionService.getRules())
					.singleElement()
					.satisfies(rule -> {
						assertThat(rule.ingredientName()).isEqualTo("Паприка");
						assertThat(rule.mode()).isEqualTo(RestrictionMode.EXCLUDE);
						assertThat(rule.note()).isEqualTo("Временно не использовать");
					});
		} finally {
			recipeRestrictionService.delete(paprikaId);
		}
	}

	@Test
	void apiDeletesRestrictionRule() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();
		recipeRestrictionService.delete(paprikaId);

		try {
			recipeRestrictionService.save(restrictionForm(
					paprikaId,
					RestrictionMode.WARN,
					"Можно редко"));

			var response = delete("/api/ingredient-restrictions/" + paprikaId);

			assertThat(response.statusCode()).isEqualTo(204);
			assertThat(recipeRestrictionService.getRules()).isEmpty();
		} finally {
			recipeRestrictionService.delete(paprikaId);
		}
	}

	@Test
	void apiListsRestrictionRulesAndFormOptions() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();
		var potatoId = ingredientIds("Картофель").getFirst();
		recipeRestrictionService.delete(paprikaId);
		recipeRestrictionService.delete(potatoId);

		try {
			recipeRestrictionService.save(restrictionForm(
					paprikaId,
					RestrictionMode.WARN,
					"Можно редко"));
			recipeRestrictionService.save(restrictionForm(
					potatoId,
					RestrictionMode.EXCLUDE,
					"Не предлагать блюда с картофелем"));

			var response = get("/api/ingredient-restrictions");

			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.headers().firstValue("Content-Type").orElse(""))
					.startsWith("application/json");
			assertThat(response.body())
					.contains("\"ingredients\":")
					.contains("\"name\":\"Картофель\"")
					.contains("\"name\":\"Паприка\"")
					.contains("\"modes\":")
					.contains("\"value\":\"WARN\",\"name\":\"Предупреждать\"")
					.contains("\"value\":\"EXCLUDE\",\"name\":\"Исключать\"")
					.contains("\"rules\":")
					.contains("\"ingredientName\":\"Картофель\"")
					.contains("\"note\":\"Не предлагать блюда с картофелем\"")
					.contains("\"ingredientName\":\"Паприка\"")
					.contains("\"note\":\"Можно редко\"")
					.contains("\"total\":2");
		} finally {
			recipeRestrictionService.delete(paprikaId);
			recipeRestrictionService.delete(potatoId);
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

	private HttpResponse<String> delete(String path)
			throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.DELETE()
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}
}
