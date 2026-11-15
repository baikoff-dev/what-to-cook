package io.github.baikoff.whattocook.test.recipe;

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
import java.util.Map;

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
import io.github.baikoff.whattocook.recipe.domain.RecipeOrigin;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;


import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeCatalogIntegrationTest extends AbstractIntegrationTest {

	@Test
	void catalogPageShowsSeededRecipes() throws IOException, InterruptedException {
		var response = get("/recipes");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("Найдено рецептов: 15")
				.contains("Ограничения")
				.contains("/preferences/restrictions")
				.contains("Гречка с тушёной курицей")
				.contains("Картофель, запечённый в духовке")
				.contains("Рис с овощами в мультиварке")
				.contains("Классические сырники")
				.contains("Шакшука");
	}

	@Test
	void catalogSearchMatchesNamesAndIngredientsIgnoringCase() {
		var byEnglishName = recipeCatalogService.search(
				new RecipeSearchCriteria("BUCKWHEAT", null, null, null, null, null, null));
		var byIngredient = recipeCatalogService.search(
				new RecipeSearchCriteria("паприк", null, null, null, null, null, null));

		assertThat(byEnglishName)
				.extracting(item -> item.nameRu())
				.containsExactly("Гречка с тушёной курицей");
		assertThat(byIngredient)
				.extracting(item -> item.nameRu())
				.containsExactly("Картофель, запечённый в духовке", "Шакшука");
	}

	@Test
	void catalogContainsTwelveClassicRecipesFromSelectedCuisines() {
		var recipes = recipeCatalogService.search(new RecipeSearchCriteria(
				null, null, null, null, null, null, RecipeOrigin.CLASSIC));

		assertThat(recipes).hasSize(12);
		assertThat(recipes)
				.extracting(item -> item.nameRu())
				.contains(
						"Классические сырники",
						"Белорусские драники",
						"Спагетти aglio e olio",
						"Классическая фриттата",
						"Рататуй",
						"Лобио по-мегрельски",
						"Помидоры с яйцом по-китайски",
						"Шакшука");
		assertThat(recipes)
				.allSatisfy(recipe -> assertThat(recipe.origin()).isEqualTo("Классический"));
	}

	@Test
	void classicRecipeContainsAuditedSourceAndSupportedSubstitutions() {
		var tomatoAndEgg = recipeCatalogService.search(new RecipeSearchCriteria(
				"Помидоры с яйцом по-китайски", null, null, null, null, null, RecipeOrigin.CLASSIC))
				.getFirst();

		var details = recipeCatalogService.getRecipe(tomatoAndEgg.id());
		var shaoxingWine = details.ingredients().stream()
				.filter(ingredient -> ingredient.name().equals("Шаосинское вино"))
				.findFirst()
				.orElseThrow();

		assertThat(details.sourceUrl())
				.isEqualTo("https://thewoksoflife.com/stir-fried-tomato-and-egg/");
		assertThat(shaoxingWine.substitutions())
				.extracting(substitution -> substitution.ingredientName())
				.containsExactly("Сухой херес", "Мирин");
	}

	@Test
	void allClassicRecipesContainAuditedKeyQuantities() {
		var expectations = Map.ofEntries(
				Map.entry("Классические сырники", new IngredientExpectation("Творог", "400", "г")),
				Map.entry("Белорусские драники", new IngredientExpectation("Картофель", "1", "кг")),
				Map.entry("Спагетти aglio e olio", new IngredientExpectation("Спагетти", "350", "г")),
				Map.entry("Классическая фриттата", new IngredientExpectation("Куриное яйцо", "8", "шт.")),
				Map.entry("Спагетти с томатным соусом", new IngredientExpectation("Помидор", "750", "г")),
				Map.entry("Рататуй", new IngredientExpectation("Кабачок", "4", "шт.")),
				Map.entry("Гратен дофинуа", new IngredientExpectation("Молоко", "150", "мл")),
				Map.entry("Лобио по-мегрельски", new IngredientExpectation("Красная фасоль сухая", "500", "г")),
				Map.entry("Чахохбили", new IngredientExpectation("Целая курица", "1.25", "кг")),
				Map.entry("Помидоры с яйцом по-китайски", new IngredientExpectation("Растительное масло", "3", "ст. л.")),
				Map.entry("Жареный рис с яйцом", new IngredientExpectation("Готовый белый рис", "3", "стакан")),
				Map.entry("Шакшука", new IngredientExpectation("Оливковое масло", "0.5", "стакан")));

		var classicRecipes = recipeCatalogService.search(new RecipeSearchCriteria(
				null, null, null, null, null, null, RecipeOrigin.CLASSIC));

		assertThat(classicRecipes).hasSize(expectations.size());
		expectations.forEach((recipeName, expectation) -> {
			var recipe = classicRecipes.stream()
					.filter(item -> item.nameRu().equals(recipeName))
					.findFirst()
					.orElseThrow();
			var details = recipeCatalogService.getRecipe(recipe.id());
			var ingredient = details.ingredients().stream()
					.filter(item -> item.name().equals(expectation.name()))
					.findFirst()
					.orElseThrow();

			assertThat(ingredient.amount()).isEqualTo(expectation.amount());
			assertThat(ingredient.unit()).isEqualTo(expectation.unit());
			assertThat(details.sourceUrl()).isNotBlank();
			assertThat(details.steps()).isNotEmpty();
		});
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
				new RecipeSearchCriteria("картофель", 50, Difficulty.EASY, ovenId, bakingId, null, null));

		assertThat(recipes)
				.extracting(item -> item.nameRu())
				.containsExactly("Картофель, запечённый в духовке");
	}

	@Test
	void catalogCanFilterByCuisineAndOrigin() {
		var eastSlavicCuisine = recipeManagementService.getCuisineOptions().stream()
				.filter(option -> option.name().equals("Восточнославянская"))
				.findFirst()
				.orElseThrow();

		var recipes = recipeCatalogService.search(new RecipeSearchCriteria(
				null,
				null,
				null,
				null,
				null,
				eastSlavicCuisine.id(),
				RecipeOrigin.PERSONAL));

		assertThat(recipes).hasSize(3);
		assertThat(recipes)
				.allSatisfy(recipe -> {
					assertThat(recipe.cuisine()).isEqualTo("Восточнославянская");
					assertThat(recipe.origin()).isEqualTo("Личный");
				});
	}

	@Test
	void catalogPageKeepsSearchParametersAndExplainsEmptyResult() throws IOException, InterruptedException {
		var searchResponse = get("/recipes?query=Buckwheat");
		var emptyResponse = get("/recipes?maxMinutes=2");

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

	private record IngredientExpectation(String name, String amount, String unit) {
	}
}
