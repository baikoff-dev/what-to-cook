package io.github.baikoff.whattocook.test.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import javax.imageio.ImageIO;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.history.service.CookingHistoryService;
import io.github.baikoff.whattocook.history.service.model.CookingRecordForm;
import io.github.baikoff.whattocook.restriction.service.model.IngredientRestrictionForm;
import io.github.baikoff.whattocook.recipe.domain.MeasurementUnit;
import io.github.baikoff.whattocook.recipe.service.RecipeCatalogService;
import io.github.baikoff.whattocook.favorite.service.RecipeFavoriteService;
import io.github.baikoff.whattocook.recipe.service.model.RecipeForm;
import io.github.baikoff.whattocook.recipe.web.RecipeFormValidator;
import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.service.RecipeManagementService;
import io.github.baikoff.whattocook.recipe.service.RecipeMatchingService;
import io.github.baikoff.whattocook.recipe.service.RecipeNotFoundException;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionService;
import io.github.baikoff.whattocook.recipe.domain.RecipeSearchCriteria;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;


import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class FavoriteIntegrationTest extends AbstractIntegrationTest {

	@Test
	void recipeCanBeAddedToAndRemovedFromFavorites() throws IOException, InterruptedException {
		var recipe = recipeCatalogService.getCatalog().getFirst();
		recipeFavoriteService.remove(recipe.id());

		try {
			var addResponse = postForm(
					"/recipes/" + recipe.id() + "/favorite",
					"returnTo=%2Frecipes%2Ffavorites");
			var favoritesResponse = get("/recipes/favorites");
			var detailsResponse = get("/recipes/" + recipe.id());

			assertThat(addResponse.statusCode()).isEqualTo(302);
			assertThat(addResponse.headers().firstValue("Location").orElseThrow())
					.contains("/recipes/favorites");
			assertThat(recipeCatalogService.getRecipe(recipe.id()).favorite()).isTrue();
			assertThat(favoritesResponse.statusCode()).isEqualTo(200);
			assertThat(favoritesResponse.body())
					.contains("Избранные рецепты")
					.contains("Рецептов в избранном: 1")
					.contains(recipe.nameRu())
					.contains("В избранном");
			assertThat(detailsResponse.body()).contains("В избранном");

			var removeResponse = postForm(
					"/recipes/" + recipe.id() + "/favorite/remove",
					"returnTo=%2Frecipes%2Ffavorites");

			assertThat(removeResponse.statusCode()).isEqualTo(302);
			assertThat(recipeCatalogService.getFavorites()).isEmpty();
			assertThat(get("/recipes/favorites").body())
					.contains("В избранном пока нет рецептов")
					.doesNotContain(recipe.nameRu());
		} finally {
			recipeFavoriteService.remove(recipe.id());
		}
	}

	@Test
	@Transactional
	void archivedFavoriteIsHiddenAndReturnsAfterRestore() {
		var recipeId = recipeCatalogService.getCatalog().getFirst().id();
		recipeFavoriteService.add(recipeId);

		assertThat(recipeCatalogService.getFavorites())
				.extracting(item -> item.id())
				.contains(recipeId);

		recipeManagementService.archive(recipeId);
		assertThat(recipeCatalogService.getFavorites())
				.extracting(item -> item.id())
				.doesNotContain(recipeId);

		recipeManagementService.restore(recipeId);
		assertThat(recipeCatalogService.getFavorites())
				.extracting(item -> item.id())
				.contains(recipeId);
	}
}
