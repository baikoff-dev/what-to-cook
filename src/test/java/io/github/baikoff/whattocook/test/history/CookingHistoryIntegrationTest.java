package io.github.baikoff.whattocook.test.history;

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

class CookingHistoryIntegrationTest extends AbstractIntegrationTest {

	@Test
	void cookingCanBeRecordedDisplayedAndDeleted() throws IOException, InterruptedException {
		var recipe = recipeCatalogService.getCatalog().getFirst();
		var form = new CookingRecordForm();
		form.setCookedOn(LocalDate.of(2026, 11, 10));
		form.setRating(4);
		form.setNote("В следующий раз добавить меньше соли");
		var recordId = cookingHistoryService.record(recipe.id(), form);

		try {
			var historyItem = cookingHistoryService.getHistory().stream()
					.filter(item -> item.id().equals(recordId))
					.findFirst()
					.orElseThrow();
			var historyResponse = get("/recipes/history");

			assertThat(historyItem.recipeName()).isEqualTo(recipe.nameRu());
			assertThat(historyItem.cookedOn()).isEqualTo(LocalDate.of(2026, 11, 10));
			assertThat(historyItem.rating()).isEqualTo(4);
			assertThat(historyItem.note()).isEqualTo("В следующий раз добавить меньше соли");
			assertThat(historyResponse.statusCode()).isEqualTo(200);
			assertThat(historyResponse.body())
					.contains("История приготовления")
					.contains(recipe.nameRu())
					.contains("10.11.2026")
					.contains("Оценка: 4 из 5")
					.contains("В следующий раз добавить меньше соли");

			var deleteResponse = postForm("/recipes/history/" + recordId + "/delete", "");
			assertThat(deleteResponse.statusCode()).isEqualTo(302);
			assertThat(cookingHistoryService.getHistory())
					.noneMatch(item -> item.id().equals(recordId));
		} finally {
			cookingHistoryService.delete(recordId);
		}
	}

	@Test
	void cookingFormRejectsFutureDateAndInvalidRating() throws IOException, InterruptedException {
		var recipeId = recipeCatalogService.getCatalog().getFirst().id();
		var futureDate = LocalDate.now().plusDays(1);

		var formResponse = get("/recipes/" + recipeId + "/cookings/new");
		var invalidResponse = postForm(
				"/recipes/" + recipeId + "/cookings/new",
				"cookedOn=" + futureDate + "&rating=6&note=");

		assertThat(formResponse.statusCode()).isEqualTo(200);
		assertThat(formResponse.body())
				.contains("Отметить приготовление")
				.contains("Добавить в историю");
		assertThat(invalidResponse.statusCode()).isEqualTo(200);
		assertThat(invalidResponse.body())
				.contains("Запись не сохранена")
				.contains("Дата приготовления не может быть в будущем")
				.contains("Оценка должна быть от 1 до 5");
	}

	@Test
	@Transactional
	void cookingHistoryIsKeptWhenRecipeIsArchived() {
		var recipeId = recipeCatalogService.getCatalog().getFirst().id();
		var form = new CookingRecordForm();
		form.setCookedOn(LocalDate.now());
		var recordId = cookingHistoryService.record(recipeId, form);

		recipeManagementService.archive(recipeId);
		var archivedItem = cookingHistoryService.getHistory().stream()
				.filter(item -> item.id().equals(recordId))
				.findFirst()
				.orElseThrow();

		assertThat(archivedItem.archived()).isTrue();
		assertThat(archivedItem.recipeId()).isEqualTo(recipeId);
	}
}
