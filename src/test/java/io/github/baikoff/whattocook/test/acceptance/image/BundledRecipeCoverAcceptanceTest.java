package io.github.baikoff.whattocook.test.acceptance.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.baikoff.whattocook.recipe.domain.RecipeOrigin;
import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class BundledRecipeCoverAcceptanceTest extends AbstractIntegrationTest {

	private static final String RECIPE_NAME = "Шакшука";
	private static final String BUNDLED_IMAGE_PATH = "bundled:recipes/classic/shakshuka.png";
	private static final String PUBLIC_IMAGE_URL = "/images/recipes/classic/shakshuka.png";
	private static final List<ExpectedCover> FIRST_BATCH_COVERS = List.of(
			new ExpectedCover("Шакшука", "/images/recipes/classic/shakshuka.jpg"),
			new ExpectedCover("Классические сырники", "/images/recipes/classic/classic-syrniki.jpg"),
			new ExpectedCover("Белорусские драники", "/images/recipes/classic/belarusian-draniki.jpg"),
			new ExpectedCover("Спагетти aglio e olio", "/images/recipes/classic/spaghetti-aglio-e-olio.jpg"));
	private static final List<ExpectedCover> SECOND_BATCH_COVERS = List.of(
			new ExpectedCover("Классическая фриттата", "/images/recipes/classic/classic-frittata.jpg"),
			new ExpectedCover("Спагетти с томатным соусом", "/images/recipes/classic/spaghetti-al-pomodoro.jpg"),
			new ExpectedCover("Рататуй", "/images/recipes/classic/classic-ratatouille.jpg"),
			new ExpectedCover("Гратен дофинуа", "/images/recipes/classic/gratin-dauphinois.jpg"));
	private static final List<ExpectedCover> FINAL_BATCH_COVERS = List.of(
			new ExpectedCover("Лобио по-мегрельски", "/images/recipes/classic/georgian-lobio.jpg"),
			new ExpectedCover("Чахохбили", "/images/recipes/classic/georgian-chakhokhbili.jpg"),
			new ExpectedCover("Помидоры с яйцом по-китайски", "/images/recipes/classic/chinese-tomato-and-egg.jpg"),
			new ExpectedCover("Жареный рис с яйцом", "/images/recipes/classic/chinese-egg-fried-rice.jpg"));

	@Autowired
	private RecipeRepository recipeRepository;

	@Test
	@DisplayName("Первая партия классических рецептов имеет доступные JPEG-обложки")
	void firstClassicRecipeBatchHasAvailableJpegCovers() throws IOException, InterruptedException {
		assertCoversAreAvailable(FIRST_BATCH_COVERS);
	}

	@Test
	@DisplayName("Вторая партия классических рецептов имеет доступные JPEG-обложки")
	void secondClassicRecipeBatchHasAvailableJpegCovers() throws IOException, InterruptedException {
		assertCoversAreAvailable(SECOND_BATCH_COVERS);
	}

	@Test
	@DisplayName("Финальная партия классических рецептов имеет доступные JPEG-обложки")
	void finalClassicRecipeBatchHasAvailableJpegCovers() throws IOException, InterruptedException {
		assertCoversAreAvailable(FINAL_BATCH_COVERS);
	}

	@Test
	@DisplayName("Все 12 классических рецептов имеют встроенные обложки")
	void allClassicRecipesHaveBundledCovers() {
		var classicRecipes = recipeRepository.findAll().stream()
				.filter(recipe -> recipe.getOrigin() == RecipeOrigin.CLASSIC)
				.toList();

		assertThat(classicRecipes).hasSize(12);
		assertThat(classicRecipes).allSatisfy(recipe ->
				assertThat(recipe.getMainImagePath()).startsWith("bundled:recipes/classic/"));
	}

	private void assertCoversAreAvailable(List<ExpectedCover> expectedCovers)
			throws IOException, InterruptedException {
		var recipes = recipeCatalogService.getCatalog();
		var catalogResponse = get("/recipes");

		assertThat(catalogResponse.statusCode()).isEqualTo(200);
		for (var expectedCover : expectedCovers) {
			var recipe = recipes.stream()
					.filter(item -> item.nameRu().equals(expectedCover.recipeName()))
					.findFirst()
					.orElseThrow();
			var imageResponse = get(expectedCover.publicUrl());

			assertThat(recipe.mainImageUrl()).isEqualTo(expectedCover.publicUrl());
			assertThat(catalogResponse.body()).contains("src=\"" + expectedCover.publicUrl() + "\"");
			assertThat(imageResponse.statusCode()).isEqualTo(200);
			assertThat(imageResponse.headers().firstValue("Content-Type"))
					.hasValueSatisfying(contentType -> assertThat(contentType).startsWith("image/jpeg"));
		}
	}

	@Test
	@DisplayName("Встроенная обложка классического рецепта отображается в каталоге и карточке")
	void bundledCoverIsShownInCatalogAndRecipeDetails() throws IOException, InterruptedException {
		var recipeSummary = recipeCatalogService.getCatalog().stream()
				.filter(recipe -> recipe.nameRu().equals(RECIPE_NAME))
				.findFirst()
				.orElseThrow();
		var recipe = recipeRepository.findById(recipeSummary.id()).orElseThrow();
		var originalImagePath = recipe.getMainImagePath();

		try {
			recipe.setMainImagePath(BUNDLED_IMAGE_PATH);
			recipeRepository.saveAndFlush(recipe);

			var catalogResponse = get("/recipes");
			var detailsResponse = get("/recipes/" + recipe.getId());

			assertThat(catalogResponse.statusCode()).isEqualTo(200);
			assertThat(catalogResponse.body()).contains("src=\"" + PUBLIC_IMAGE_URL + "\"");
			assertThat(detailsResponse.statusCode()).isEqualTo(200);
			assertThat(detailsResponse.body()).contains("src=\"" + PUBLIC_IMAGE_URL + "\"");
		} finally {
			var savedRecipe = recipeRepository.findById(recipe.getId()).orElseThrow();
			savedRecipe.setMainImagePath(originalImagePath);
			recipeRepository.saveAndFlush(savedRecipe);
		}
	}

	private record ExpectedCover(String recipeName, String publicUrl) {
	}
}
