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
import io.github.baikoff.whattocook.recipe.RecipeMatchingService;
import io.github.baikoff.whattocook.recipe.RecipeNotFoundException;
import io.github.baikoff.whattocook.recipe.RecipeSearchCriteria;
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
	private RecipeMatchingService recipeMatchingService;

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
	void catalogSearchMatchesNamesAndIngredientsIgnoringCase() {
		var byEnglishName = recipeCatalogService.search(
				new RecipeSearchCriteria("BUCKWHEAT", null, null, null, null));
		var byIngredient = recipeCatalogService.search(
				new RecipeSearchCriteria("паприк", null, null, null, null));

		assertThat(byEnglishName)
				.extracting(item -> item.nameRu())
				.containsExactly("Гречка с тушёной курицей");
		assertThat(byIngredient)
				.extracting(item -> item.nameRu())
				.containsExactly("Картофель, запечённый в духовке");
	}

	@Test
	void catalogFiltersCanBeCombined() {
		var ovenId = recipeManagementService.getEquipmentOptions().stream()
				.filter(item -> item.name().equals("Электродуховка"))
				.findFirst()
				.orElseThrow()
				.id();
		var bakingId = recipeManagementService.getCookingMethodOptions().stream()
				.filter(item -> item.name().equals("Запекание"))
				.findFirst()
				.orElseThrow()
				.id();

		var recipes = recipeCatalogService.search(
				new RecipeSearchCriteria("картофель", 50, Difficulty.EASY, ovenId, bakingId));

		assertThat(recipes)
				.extracting(item -> item.nameRu())
				.containsExactly("Картофель, запечённый в духовке");
	}

	@Test
	void catalogPageKeepsSearchParametersAndExplainsEmptyResult() throws IOException, InterruptedException {
		var searchResponse = get("/recipes?query=Buckwheat");
		var emptyResponse = get("/recipes?maxMinutes=49");

		assertThat(searchResponse.statusCode()).isEqualTo(200);
		assertThat(searchResponse.body())
				.contains("Найдено рецептов: 1")
				.contains("value=\"Buckwheat\"")
				.contains("Гречка с тушёной курицей")
				.doesNotContain("Картофель, запечённый в духовке");
		assertThat(emptyResponse.statusCode()).isEqualTo(200);
		assertThat(emptyResponse.body())
				.contains("Найдено рецептов: 0")
				.contains("По заданным условиям рецепты не найдены")
				.contains("Сбросить");
	}

	@Test
	void matchingRanksCompleteRecipeFirstAndShowsMissingIngredients() {
		var selectedIds = ingredientIds(
				"Картофель",
				"Растительное масло",
				"Паприка",
				"Чеснок",
				"Соль");

		var matching = recipeMatchingService.match(selectedIds);
		var potatoRecipe = matching.recipes().getFirst();
		var buckwheatRecipe = matching.recipes().stream()
				.filter(recipe -> recipe.nameRu().equals("Гречка с тушёной курицей"))
				.findFirst()
				.orElseThrow();

		assertThat(potatoRecipe.nameRu()).isEqualTo("Картофель, запечённый в духовке");
		assertThat(potatoRecipe.matchPercentage()).isEqualTo(100);
		assertThat(potatoRecipe.canCook()).isTrue();
		assertThat(potatoRecipe.missingIngredients()).isEmpty();
		assertThat(buckwheatRecipe.matchedIngredientCount()).isEqualTo(2);
		assertThat(buckwheatRecipe.requiredIngredientCount()).isEqualTo(7);
		assertThat(buckwheatRecipe.matchPercentage()).isEqualTo(29);
		assertThat(buckwheatRecipe.missingIngredients()).contains("Куриное филе", "Гречка");
	}

	@Test
	void matchingHidesRecipesWithoutSelectedRequiredIngredients() {
		var paprikaId = ingredientIds("Паприка").getFirst();

		var matching = recipeMatchingService.match(java.util.List.of(paprikaId));

		assertThat(matching.recipes())
				.extracting(recipe -> recipe.nameRu())
				.containsExactly("Картофель, запечённый в духовке");
	}

	@Test
	@Transactional
	void optionalIngredientsDoNotReduceMatchAndArchivedRecipesAreExcluded() {
		var form = validRecipeForm();
		form.getIngredients().getFirst().setName("Обязательная тестовая крупа");
		var optionalIngredient = new RecipeForm.IngredientRow();
		optionalIngredient.setName("Необязательная тестовая приправа");
		optionalIngredient.setAmount(BigDecimal.ONE);
		optionalIngredient.setUnit(MeasurementUnit.PINCH);
		optionalIngredient.setOptional(true);
		form.getIngredients().add(optionalIngredient);
		var recipeId = recipeManagementService.create(form);
		var requiredIngredientId = ingredientIds("Обязательная тестовая крупа").getFirst();

		var matchedRecipe = recipeMatchingService.match(java.util.List.of(requiredIngredientId)).recipes().stream()
				.filter(recipe -> recipe.id().equals(recipeId))
				.findFirst()
				.orElseThrow();

		assertThat(matchedRecipe.requiredIngredientCount()).isEqualTo(1);
		assertThat(matchedRecipe.matchPercentage()).isEqualTo(100);
		assertThat(matchedRecipe.missingIngredients()).isEmpty();

		recipeManagementService.archive(recipeId);
		assertThat(recipeMatchingService.match(java.util.List.of(requiredIngredientId)).recipes()).isEmpty();
	}

	@Test
	void matchingPagePromptsForProductsAndDisplaysCalculatedResults() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();

		var emptyResponse = get("/recipes/match");
		var matchingResponse = get("/recipes/match?ingredientIds=" + paprikaId);

		assertThat(emptyResponse.statusCode()).isEqualTo(200);
		assertThat(emptyResponse.body())
				.contains("Выбери хотя бы один продукт")
				.contains("Найти продукт в списке");
		assertThat(matchingResponse.statusCode()).isEqualTo(200);
		assertThat(matchingResponse.body())
				.contains("Результаты подбора")
				.contains("Картофель, запечённый в духовке")
				.contains("Не хватает:")
				.contains("aria-valuenow")
				.doesNotContain("Гречка с тушёной курицей")
				.doesNotContain("Рис с овощами в мультиварке");
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
	void recipePageRecalculatesIngredientsForSelectedServings() throws IOException, InterruptedException {
		var recipe = recipeCatalogService.getCatalog()
				.stream()
				.filter(item -> item.nameRu().equals("Гречка с тушёной курицей"))
				.findFirst()
				.orElseThrow();

		var recalculatedRecipe = recipeCatalogService.getRecipe(recipe.id(), 3);
		var chicken = recalculatedRecipe.ingredients()
				.stream()
				.filter(ingredient -> ingredient.name().equals("Куриное филе"))
				.findFirst()
				.orElseThrow();
		var response = get("/recipes/" + recipe.id() + "?servings=3");

		assertThat(recalculatedRecipe.baseServings()).isEqualTo(2);
		assertThat(recalculatedRecipe.servings()).isEqualTo(3);
		assertThat(chicken.amount()).isEqualTo("450");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("Пересчитано на 3 порц.")
				.contains("value=\"3\"")
				.contains("450");
	}

	@Test
	void recipePageShowsErrorForUnsupportedServingCount() throws IOException, InterruptedException {
		var recipeId = recipeCatalogService.getCatalog().getFirst().id();

		var response = get("/recipes/" + recipeId + "?servings=0");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("Количество порций должно быть от 1 до 100");
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
	void invalidRecipeFormShowsErrorSummary() throws IOException, InterruptedException {
		var response = postForm("/recipes/new", "");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("data-error-summary")
				.contains("Рецепт не сохранён")
				.contains("Укажи русское название")
				.contains("Добавь описание");
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

	private java.util.List<Long> ingredientIds(String... names) {
		var requestedNames = java.util.Set.of(names);
		var ids = recipeMatchingService.match(null).ingredients().stream()
				.filter(ingredient -> requestedNames.contains(ingredient.name()))
				.map(ingredient -> ingredient.id())
				.toList();
		assertThat(ids).hasSize(requestedNames.size());
		return ids;
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

	private HttpResponse<String> postForm(String path, String body) throws IOException, InterruptedException {
		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
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
