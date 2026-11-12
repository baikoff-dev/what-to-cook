package io.github.baikoff.whattocook;

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

import io.github.baikoff.whattocook.recipe.Difficulty;
import io.github.baikoff.whattocook.recipe.MeasurementUnit;
import io.github.baikoff.whattocook.recipe.RecipeCatalogService;
import io.github.baikoff.whattocook.recipe.RecipeForm;
import io.github.baikoff.whattocook.recipe.RecipeFormValidator;
import io.github.baikoff.whattocook.recipe.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.RecipeManagementService;
import io.github.baikoff.whattocook.recipe.RecipeNotFoundException;
import io.github.baikoff.whattocook.recipe.RecipeStatus;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.images.directory=target/test-images")
@Import(WhatToCookApplicationTests.PostgreSQLTestConfiguration.class)
class WhatToCookApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private Flyway flyway;

	@Autowired
	private RecipeCatalogService recipeCatalogService;

	@Autowired
	private RecipeManagementService recipeManagementService;

	@Autowired
	private RecipeFormValidator recipeFormValidator;

	@Autowired
	private RecipeImageStorage recipeImageStorage;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayMigrationsAreApplied() {
		assertThat(flyway.info().applied())
				.extracting(migration -> migration.getVersion().getVersion())
				.containsExactly("1", "2", "3");
	}

	@Test
	void homePageIsAvailable() throws IOException, InterruptedException {
		var response = get("/");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("Что приготовить?");
		assertThat(response.body()).contains("Открыть каталог");
	}

	@Test
	void catalogPageShowsSeededRecipes() throws IOException, InterruptedException {
		var response = get("/recipes");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("Найдено рецептов: 3")
				.contains("Гречка с тушёной курицей")
				.contains("Картофель, запечённый в духовке")
				.contains("Рис с овощами в мультиварке");
	}

	@Test
	void recipePageShowsIngredientsAndSteps() throws IOException, InterruptedException {
		var recipe = recipeCatalogService.getCatalog()
				.stream()
				.filter(item -> item.nameRu().equals("Гречка с тушёной курицей"))
				.findFirst()
				.orElseThrow();

		var response = get("/recipes/" + recipe.id());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("Куриное филе")
				.contains("300")
				.contains("Тушение")
				.contains("Промыть гречку");
	}

	@Test
	void missingRecipeReturnsCustomNotFoundPage() throws IOException, InterruptedException {
		var response = get("/recipes/999999");

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(response.body())
				.contains("Рецепт не найден")
				.contains("Вернуться в каталог");
	}

	@Test
	void serviceReturnsCompleteRecipeDetails() {
		var recipeId = recipeCatalogService.getCatalog().getFirst().id();

		var recipe = recipeCatalogService.getRecipe(recipeId);

		assertThat(recipe.ingredients()).isNotEmpty();
		assertThat(recipe.steps()).isNotEmpty();
		assertThat(recipe.equipment()).isNotEmpty();
		assertThat(recipe.cookingMethods()).isNotEmpty();
		assertThat(recipe.totalMinutes())
				.isEqualTo(recipe.preparationMinutes() + recipe.cookingMinutes());
	}

	@Test
	void createAndEditFormsAreAvailable() throws IOException, InterruptedException {
		var recipeId = recipeCatalogService.getCatalog().getFirst().id();

		var createResponse = get("/recipes/new");
		var editResponse = get("/recipes/" + recipeId + "/edit");

		assertThat(createResponse.statusCode()).isEqualTo(200);
		assertThat(createResponse.body())
				.contains("Новый рецепт")
				.contains("Добавить ингредиент")
				.contains("Добавить шаг");
		assertThat(editResponse.statusCode()).isEqualTo(200);
		assertThat(editResponse.body()).contains("Редактирование рецепта");
	}

	@Test
	@Transactional
	void recipeCanBeCreatedUpdatedArchivedAndRestored() throws IOException {
		var form = validRecipeForm();
		form.setMainImage(validPng());

		var recipeId = recipeManagementService.create(form);
		var createdRecipe = recipeCatalogService.getRecipe(recipeId);
		var editForm = recipeManagementService.getForm(recipeId);

		assertThat(createdRecipe.nameRu()).isEqualTo("Тестовый суп");
		assertThat(createdRecipe.mainImageUrl()).startsWith("/user-images/recipes/");
		assertThat(editForm.getExistingMainImagePath()).isNotBlank();
		assertThat(recipeImageStorage.getRootDirectory().resolve(editForm.getExistingMainImagePath())).exists();

		editForm.setNameRu("Обновлённый тестовый суп");
		recipeManagementService.update(recipeId, editForm);
		assertThat(recipeCatalogService.getRecipe(recipeId).nameRu()).isEqualTo("Обновлённый тестовый суп");

		recipeManagementService.archive(recipeId);
		assertThatThrownBy(() -> recipeCatalogService.getRecipe(recipeId))
				.isInstanceOf(RecipeNotFoundException.class);
		assertThat(recipeCatalogService.getArchive())
				.extracting(item -> item.id())
				.contains(recipeId);

		recipeManagementService.restore(recipeId);
		assertThat(recipeCatalogService.getRecipe(recipeId).status()).isEqualTo("Не проверен");
	}

	@Test
	void formValidatorRejectsDuplicateIngredientsAndInvalidImage() {
		var form = validRecipeForm();
		var duplicate = new RecipeForm.IngredientRow();
		duplicate.setName(" тестовая крупа ");
		duplicate.setAmount(BigDecimal.ONE);
		duplicate.setUnit(MeasurementUnit.GRAM);
		form.getIngredients().add(duplicate);
		form.setMainImage(new MockMultipartFile(
				"mainImage", "not-image.txt", "text/plain", "not an image".getBytes()));
		var errors = new BeanPropertyBindingResult(form, "recipeForm");

		recipeFormValidator.validate(form, errors);

		assertThat(errors.getFieldErrors("ingredients[1].name")).isNotEmpty();
		assertThat(errors.getFieldErrors("mainImage")).isNotEmpty();
	}

	private RecipeForm validRecipeForm() {
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

	private MockMultipartFile validPng() throws IOException {
		var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		var output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		return new MockMultipartFile("mainImage", "dish.png", "image/png", output.toByteArray());
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.GET()
				.build();

		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class PostgreSQLTestConfiguration {

		@Bean
		@ServiceConnection
		PostgreSQLContainer postgresContainer() {
			return new PostgreSQLContainer("postgres:18.6-alpine")
					.withDatabaseName("what_to_cook_test");
		}
	}
}
