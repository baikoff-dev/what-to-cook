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

class RecipeMatchingIntegrationTest extends AbstractIntegrationTest {

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
}
