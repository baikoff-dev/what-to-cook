package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeArchiveRestAcceptanceTest extends AbstractIntegrationTest {

	@Test
	void apiArchivesListsAndRestoresRecipeWithoutLosingItsWorkingStatus()
			throws IOException, InterruptedException {
		var recipe = recipeCatalogService.getCatalog().getFirst();
		var recipeId = recipe.id();
		var workingStatus = recipe.status();

		try {
			var archiveResponse = send("PUT", "/api/recipes/" + recipeId + "/archive");

			assertThat(archiveResponse.statusCode()).isEqualTo(200);
			assertThat(archiveResponse.body()).isEqualTo("{\"archived\":true}");
			assertThat(recipeCatalogService.getArchive())
					.anyMatch(item -> item.id().equals(recipeId) && "В архиве".equals(item.status()));

			var archiveCatalogResponse = get("/api/recipes/archive");

			assertThat(archiveCatalogResponse.statusCode()).isEqualTo(200);
			assertThat(archiveCatalogResponse.body())
					.contains("\"id\":" + recipeId)
					.contains("\"name\":\"" + recipe.nameRu() + "\"")
					.contains("\"status\":\"В архиве\"");
			assertThat(get("/api/recipes/" + recipeId).statusCode()).isEqualTo(404);

			var restoreResponse = send("DELETE", "/api/recipes/" + recipeId + "/archive");

			assertThat(restoreResponse.statusCode()).isEqualTo(200);
			assertThat(restoreResponse.body()).isEqualTo("{\"archived\":false}");
			assertThat(recipeCatalogService.getArchive())
					.noneMatch(item -> item.id().equals(recipeId));
			assertThat(recipeCatalogService.getRecipe(recipeId).status()).isEqualTo(workingStatus);
		} finally {
			recipeManagementService.restore(recipeId);
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
