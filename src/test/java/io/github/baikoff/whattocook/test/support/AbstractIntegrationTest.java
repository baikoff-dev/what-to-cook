package io.github.baikoff.whattocook.test.support;

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

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.images.directory=target/test-images")
@Import(PostgreSQLTestConfiguration.class)
public abstract class AbstractIntegrationTest {

	@LocalServerPort
	protected int port;

	@Autowired
	protected Flyway flyway;

	@Autowired
	protected RecipeCatalogService recipeCatalogService;

	@Autowired
	protected RecipeManagementService recipeManagementService;

	@Autowired
	protected RecipeMatchingService recipeMatchingService;

	@Autowired
	protected RecipeFormValidator recipeFormValidator;

	@Autowired
	protected RecipeFavoriteService recipeFavoriteService;

	@Autowired
	protected CookingHistoryService cookingHistoryService;

	@Autowired
	protected RecipeRestrictionService recipeRestrictionService;

	@Autowired
	protected RecipeImageStorage recipeImageStorage;

	protected RecipeForm validRecipeForm() {
		var form = RecipeForm.empty();
		form.setNameRu("Тестовый суп");
		form.setDescription("Рецепт, созданный интеграционным тестом");
		form.setServings(2);
		form.setPreparationMinutes(10);
		form.setCookingMinutes(20);
		form.setDifficulty(Difficulty.EASY);
		form.setStatus(RecipeStatus.UNTESTED);
		form.setEquipmentIds(recipeManagementService.getEquipmentOptions().stream().limit(1).map(item -> item.id()).toList());
		form.setCookingMethodIds(recipeManagementService.getCookingMethodOptions().stream().limit(1).map(item -> item.id()).toList());
		form.getIngredients().getFirst().setName("Тестовая крупа");
		form.getIngredients().getFirst().setAmount(new BigDecimal("100"));
		form.getIngredients().getFirst().setUnit(MeasurementUnit.GRAM);
		form.getSteps().getFirst().setInstruction("Проверить работу сохранения рецепта.");
		return form;
	}

	protected IngredientRestrictionForm restrictionForm(
			long ingredientId,
			RestrictionMode mode,
			String note) {
		var form = new IngredientRestrictionForm();
		form.setIngredientId(ingredientId);
		form.setMode(mode);
		form.setNote(note);
		return form;
	}

	protected java.util.List<Long> ingredientIds(String... names) {
		var requestedNames = java.util.Set.of(names);
		var ids = recipeMatchingService.match(null).ingredients().stream()
				.filter(ingredient -> requestedNames.contains(ingredient.name()))
				.map(ingredient -> ingredient.id())
				.toList();
		assertThat(ids).hasSize(requestedNames.size());
		return ids;
	}

	protected MockMultipartFile validPng() throws IOException {
		var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		var output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		return new MockMultipartFile("mainImage", "dish.png", "image/png", output.toByteArray());
	}

	protected HttpResponse<String> get(String path) throws IOException, InterruptedException {
		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.GET()
				.build();

		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	protected HttpResponse<String> postForm(String path, String body) throws IOException, InterruptedException {
		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
