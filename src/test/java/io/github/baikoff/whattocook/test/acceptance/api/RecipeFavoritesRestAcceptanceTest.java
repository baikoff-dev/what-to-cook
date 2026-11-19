package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeFavoritesRestAcceptanceTest extends AbstractIntegrationTest {

	@Test
	void apiReturnsOnlyFavoriteRecipesForFrontend() throws IOException, InterruptedException {
		var recipes = recipeCatalogService.getCatalog();
		var favoriteRecipe = recipes.getFirst();
		var regularRecipe = recipes.get(1);
		recipeFavoriteService.getFavoriteRecipeIds().forEach(recipeFavoriteService::remove);
		recipeFavoriteService.add(favoriteRecipe.id());

		try {
			var response = get("/api/recipes/favorites");

			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.headers().firstValue("Content-Type").orElse(""))
					.startsWith("application/json");
			assertThat(response.body())
					.contains("\"total\":1")
					.contains("\"name\":\"" + favoriteRecipe.nameRu() + "\"")
					.contains("\"favorite\":true")
					.doesNotContain("\"name\":\"" + regularRecipe.nameRu() + "\"");
		} finally {
			recipeFavoriteService.remove(favoriteRecipe.id());
		}
	}
}
