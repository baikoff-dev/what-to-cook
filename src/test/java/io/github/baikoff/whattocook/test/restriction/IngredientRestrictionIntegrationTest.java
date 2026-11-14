package io.github.baikoff.whattocook.test.restriction;

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

class IngredientRestrictionIntegrationTest extends AbstractIntegrationTest {

	@Test
	void warningRestrictionKeepsRecipeVisibleAndExplainsTheRule() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();
		var form = restrictionForm(paprikaId, RestrictionMode.WARN, "Можно редко");
		recipeRestrictionService.delete(paprikaId);

		try {
			assertThat(recipeRestrictionService.save(form)).isTrue();
			var catalogItem = recipeCatalogService.search(
					new RecipeSearchCriteria("паприка", null, null, null, null)).getFirst();
			var matchItem = recipeMatchingService.match(java.util.List.of(paprikaId)).recipes().getFirst();
			var details = recipeCatalogService.getRecipe(catalogItem.id());
			var restrictionsPage = get("/preferences/restrictions");
			var recipePage = get("/recipes/" + catalogItem.id());

			assertThat(catalogItem.restrictionWarnings())
					.singleElement()
					.satisfies(warning -> {
						assertThat(warning.ingredientName()).isEqualTo("Паприка");
						assertThat(warning.note()).isEqualTo("Можно редко");
						assertThat(warning.excluding()).isFalse();
					});
			assertThat(matchItem.restrictionWarnings()).hasSize(1);
			assertThat(details.excludedByRestriction()).isFalse();
			assertThat(details.restrictionWarnings()).hasSize(1);
			assertThat(restrictionsPage.statusCode()).isEqualTo(200);
			assertThat(restrictionsPage.body())
					.contains("Ограничения по ингредиентам")
					.contains("Паприка")
					.contains("Можно редко")
					.contains("Предупреждать");
			assertThat(recipePage.body())
					.contains("Обрати внимание на ограничения")
					.contains("Паприка")
					.contains("Можно редко");
		} finally {
			recipeRestrictionService.delete(paprikaId);
		}
	}

	@Test
	void exclusionRestrictionHidesRecipeFromResultsButNotDirectPage() throws IOException, InterruptedException {
		var potatoId = ingredientIds("Картофель").getFirst();
		var potatoRecipe = recipeCatalogService.search(
				new RecipeSearchCriteria("запечённый", null, null, null, null)).getFirst();
		recipeFavoriteService.add(potatoRecipe.id());
		recipeRestrictionService.delete(potatoId);

		try {
			recipeRestrictionService.save(restrictionForm(
					potatoId,
					RestrictionMode.EXCLUDE,
					"Не предлагать блюда с картофелем"));

			assertThat(recipeCatalogService.search(
					new RecipeSearchCriteria("запечённый", null, null, null, null))).isEmpty();
			assertThat(recipeCatalogService.getFavorites())
					.extracting(item -> item.id())
					.doesNotContain(potatoRecipe.id());
			assertThat(recipeMatchingService.match(java.util.List.of(potatoId)).recipes())
					.extracting(item -> item.id())
					.doesNotContain(potatoRecipe.id());

			var details = recipeCatalogService.getRecipe(potatoRecipe.id());
			var response = get("/recipes/" + potatoRecipe.id());
			assertThat(details.excludedByRestriction()).isTrue();
			assertThat(details.restrictionWarnings())
					.extracting(warning -> warning.ingredientName())
					.contains("Картофель");
			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.body())
					.contains("Рецепт исключён личным правилом")
					.contains("Не предлагать блюда с картофелем");
		} finally {
			recipeRestrictionService.delete(potatoId);
			recipeFavoriteService.remove(potatoRecipe.id());
		}
	}

	@Test
	@Transactional
	void optionalIngredientAlsoTriggersExclusion() {
		var form = validRecipeForm();
		form.setNameRu("Рецепт с необязательным аллергеном");
		var optionalIngredient = new RecipeForm.IngredientRow();
		optionalIngredient.setName("Тестовый необязательный аллерген");
		optionalIngredient.setAmount(BigDecimal.ONE);
		optionalIngredient.setUnit(MeasurementUnit.GRAM);
		optionalIngredient.setOptional(true);
		form.getIngredients().add(optionalIngredient);
		var recipeId = recipeManagementService.create(form);
		var allergenId = ingredientIds("Тестовый необязательный аллерген").getFirst();
		recipeRestrictionService.save(restrictionForm(allergenId, RestrictionMode.EXCLUDE, null));

		assertThat(recipeCatalogService.search(
				new RecipeSearchCriteria("необязательным аллергеном", null, null, null, null))).isEmpty();
		var details = recipeCatalogService.getRecipe(recipeId);
		assertThat(details.excludedByRestriction()).isTrue();
		assertThat(details.restrictionWarnings())
				.singleElement()
				.satisfies(warning -> assertThat(warning.optional()).isTrue());
	}

	@Test
	void restrictionFormExplainsMissingRequiredFields() throws IOException, InterruptedException {
		var response = postForm("/preferences/restrictions", "");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("Правило не сохранено")
				.contains("Выбери ингредиент")
				.contains("Выбери режим ограничения");
	}
}
