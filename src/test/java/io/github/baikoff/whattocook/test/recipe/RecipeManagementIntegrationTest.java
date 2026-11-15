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
import io.github.baikoff.whattocook.recipe.domain.RecipeOrigin;
import io.github.baikoff.whattocook.recipe.domain.SubstitutionType;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;


import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeManagementIntegrationTest extends AbstractIntegrationTest {

	@Test
	void createAndEditFormsAreAvailable() throws IOException, InterruptedException {
		var recipeId = recipeCatalogService.getCatalog().getFirst().id();

		var createResponse = get("/recipes/new");
		var editResponse = get("/recipes/" + recipeId + "/edit");

		assertThat(createResponse.statusCode()).isEqualTo(200);
		assertThat(createResponse.body())
				.contains("Новый рецепт")
				.contains("Добавить ингредиент")
				.contains("Добавить шаг")
				.contains("Допустимые замены")
				.contains("Происхождение рецепта");
		assertThat(editResponse.statusCode()).isEqualTo(200);
		assertThat(editResponse.body()).contains("Редактирование рецепта");
	}

	@Test
	@Transactional
	void recipeStoresCuisineOriginAndIngredientSubstitution() {
		var form = validRecipeForm();
		var italianCuisine = recipeManagementService.getCuisineOptions().stream()
				.filter(option -> option.name().equals("Итальянская"))
				.findFirst()
				.orElseThrow();
		form.setCuisineId(italianCuisine.id());
		form.setOrigin(RecipeOrigin.CLASSIC);

		var substitution = new RecipeForm.SubstitutionRow();
		substitution.setOriginalIngredientName("Тестовая крупа");
		substitution.setSubstituteIngredientName("Другая тестовая крупа");
		substitution.setAmount(new BigDecimal("120"));
		substitution.setUnit(MeasurementUnit.GRAM);
		substitution.setType(SubstitutionType.ADAPTED);
		substitution.setImpactNote("Вкус станет мягче, а текстура — более рассыпчатой.");
		form.getSubstitutions().add(substitution);

		var recipeId = recipeManagementService.create(form);
		var recipe = recipeCatalogService.getRecipe(recipeId, 4);
		var savedSubstitution = recipe.ingredients().getFirst().substitutions().getFirst();

		assertThat(recipe.cuisine()).isEqualTo("Итальянская");
		assertThat(recipe.origin()).isEqualTo("Классический");
		assertThat(savedSubstitution.ingredientName()).isEqualTo("Другая тестовая крупа");
		assertThat(savedSubstitution.amount()).isEqualTo("240");
		assertThat(savedSubstitution.type()).isEqualTo("Адаптированный вариант");
		assertThat(savedSubstitution.impactNote()).contains("текстура");
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

	@Test
	void formValidatorRejectsInvalidIngredientSubstitution() {
		var form = validRecipeForm();
		var substitution = new RecipeForm.SubstitutionRow();
		substitution.setOriginalIngredientName("Несуществующий ингредиент");
		substitution.setSubstituteIngredientName("Несуществующий ингредиент");
		substitution.setAmount(BigDecimal.ONE);
		substitution.setUnit(MeasurementUnit.GRAM);
		substitution.setType(SubstitutionType.CLOSE);
		substitution.setImpactNote("Проверка валидации");
		form.getSubstitutions().add(substitution);
		var errors = new BeanPropertyBindingResult(form, "recipeForm");

		recipeFormValidator.validate(form, errors);

		assertThat(errors.getFieldErrors("substitutions[0].originalIngredientName")).isNotEmpty();
		assertThat(errors.getFieldErrors("substitutions[0].substituteIngredientName")).isNotEmpty();
	}
}
