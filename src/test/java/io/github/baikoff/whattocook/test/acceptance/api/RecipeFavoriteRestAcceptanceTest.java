package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeFavoriteRestAcceptanceTest extends AbstractIntegrationTest {

	@Test
	void apiAddsRecipeToFavoritesAndRemovesIt() throws IOException, InterruptedException {
		var recipe = recipeCatalogService.getCatalog().getFirst();
		recipeFavoriteService.remove(recipe.id());

		try {
			var addResponse = send("PUT", "/api/recipes/" + recipe.id() + "/favorite");

			assertThat(addResponse.statusCode()).isEqualTo(200);
			assertThat(addResponse.body()).isEqualTo("{\"favorite\":true}");
			assertThat(recipeCatalogService.getRecipe(recipe.id()).favorite()).isTrue();

			var removeResponse = send("DELETE", "/api/recipes/" + recipe.id() + "/favorite");

			assertThat(removeResponse.statusCode()).isEqualTo(200);
			assertThat(removeResponse.body()).isEqualTo("{\"favorite\":false}");
			assertThat(recipeCatalogService.getRecipe(recipe.id()).favorite()).isFalse();
		} finally {
			recipeFavoriteService.remove(recipe.id());
		}
	}

	private HttpResponse<String> send(String method, String path)
			throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.method(method, HttpRequest.BodyPublishers.noBody())
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}
}
