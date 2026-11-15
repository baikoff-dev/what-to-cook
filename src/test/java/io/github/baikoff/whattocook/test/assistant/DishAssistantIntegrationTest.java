package io.github.baikoff.whattocook.test.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.assistant.service.model.AssistantCriteria;
import io.github.baikoff.whattocook.assistant.service.model.AssistantGoal;
import io.github.baikoff.whattocook.history.service.model.CookingRecordForm;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.RecipeSearchCriteria;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;
import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class DishAssistantIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private DishRecommendationService recommendationService;

	@Test
	void assistantPageIsAvailableFromMainNavigation() throws IOException, InterruptedException {
		var assistantResponse = get("/assistant");
		var homeResponse = get("/");

		assertThat(assistantResponse.statusCode()).isEqualTo(200);
		assertThat(assistantResponse.body())
				.contains("Помощник выбора")
				.contains("Какая цель сегодня?")
				.contains("Приготовить побыстрее")
				.contains("Способ приготовления")
				.contains("Какие продукты есть дома?")
				.contains("Не знаю, предложи сам")
				.contains("aria-current=\"page\"");
		assertThat(homeResponse.body())
				.contains("Помоги выбрать блюдо")
				.contains("href=\"/assistant\"");
	}

	@Test
	void assistantUsesProductsTimeEquipmentAndDifficulty() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();
		var ovenId = recommendationService.getEquipmentOptions().stream()
				.filter(option -> option.name().equals("Электродуховка"))
				.findFirst()
				.orElseThrow()
				.id();
		var bakingId = recommendationService.getCookingMethodOptions().stream()
				.filter(option -> option.name().equals("Запекание"))
				.findFirst()
				.orElseThrow()
				.id();

		var recommendations = recommendationService.recommend(new AssistantCriteria(
				List.of(paprikaId),
				50,
				ovenId,
				bakingId,
				Difficulty.EASY,
				AssistantGoal.USE_PRODUCTS,
				false));

		assertThat(recommendations)
				.singleElement()
				.satisfies(recipe -> {
					assertThat(recipe.nameRu()).isEqualTo("Картофель, запечённый в духовке");
					assertThat(recipe.matchPercentage()).isEqualTo(20);
					assertThat(recipe.missingIngredients()).contains("Картофель", "Чеснок");
					assertThat(recipe.reasons())
							.contains("Есть 1 из 5 обязательных продуктов")
							.contains("Подходит для оборудования: Электродуховка")
							.contains("Способ приготовления: Запекание")
							.contains("Приоритет отдан совпадению имеющихся продуктов");
				});
		assertThat(recommendationService.recommend(new AssistantCriteria(
				List.of(paprikaId), null, null, null, null, AssistantGoal.BALANCED, false)))
				.extracting(recipe -> recipe.nameRu())
				.containsExactly("Картофель, запечённый в духовке", "Шакшука");

		var response = get("/assistant?ingredientIds=" + paprikaId
				+ "&maxMinutes=50&equipmentId=" + ovenId
				+ "&cookingMethodId=" + bakingId
				+ "&maxDifficulty=EASY&goal=USE_PRODUCTS&mode=PREFERENCES");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("Подходящие блюда")
				.contains("Картофель, запечённый в духовке")
				.contains("Есть 1 из 5 обязательных продуктов");
	}

	@Test
	@Transactional
	void quickGoalPlacesTheFastestRecipeFirst() {
		var form = validRecipeForm();
		form.setNameRu("Очень быстрый тестовый рецепт");
		form.setPreparationMinutes(1);
		form.setCookingMinutes(2);
		var recipeId = recipeManagementService.create(form);

		var recommendations = recommendationService.recommend(new AssistantCriteria(
				List.of(), null, null, null, null, AssistantGoal.QUICK, false));

		assertThat(recommendations.getFirst().id()).isEqualTo(recipeId);
		assertThat(recommendations.getFirst().reasons())
				.contains("Общее время приготовления — 3 мин");
	}

	@Test
	@Transactional
	void favoriteGoalUsesFavoritesAndAveragePersonalRating() {
		var riceRecipe = recipeCatalogService.search(
				new RecipeSearchCriteria("рис с овощами", null, null, null, null, null, null)).getFirst();
		recipeFavoriteService.add(riceRecipe.id());
		var firstCooking = new CookingRecordForm();
		firstCooking.setCookedOn(LocalDate.now().minusDays(2));
		firstCooking.setRating(4);
		cookingHistoryService.record(riceRecipe.id(), firstCooking);
		var secondCooking = new CookingRecordForm();
		secondCooking.setCookedOn(LocalDate.now().minusDays(1));
		secondCooking.setRating(5);
		cookingHistoryService.record(riceRecipe.id(), secondCooking);

		var recommendations = recommendationService.recommend(new AssistantCriteria(
				List.of(), null, null, null, null, AssistantGoal.FAVORITE, false));

		assertThat(recommendations.getFirst().id()).isEqualTo(riceRecipe.id());
		assertThat(recommendations.getFirst().favorite()).isTrue();
		assertThat(recommendations.getFirst().averageRating()).isEqualTo(4.5);
		assertThat(recommendations.getFirst().reasons())
				.contains("Рецепт добавлен в избранное")
				.contains("Средняя личная оценка — 4,5 из 5");
	}

	@Test
	void useProductsGoalExplainsThatProductsAreRequired() throws IOException, InterruptedException {
		var response = get("/assistant?goal=USE_PRODUCTS&mode=PREFERENCES");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("Для цели «Использовать имеющиеся продукты» выбери хотя бы один продукт")
				.doesNotContain("Подходящие блюда");
	}

	@Test
	void surpriseModePrefersRecipesThatWereNotCookedRecently() {
		var cookedRecipe = recipeCatalogService.search(
				new RecipeSearchCriteria("гречка", null, null, null, null, null, null)).getFirst();
		var form = new CookingRecordForm();
		form.setCookedOn(LocalDate.now());
		var recordId = cookingHistoryService.record(cookedRecipe.id(), form);

		try {
			var recommendations = recommendationService.recommend(AssistantCriteria.surpriseMe());

			assertThat(recommendations).hasSize(3);
			assertThat(recommendations)
					.noneSatisfy(recipe -> assertThat(recipe.id()).isEqualTo(cookedRecipe.id()));
			assertThat(recommendations)
					.allSatisfy(recipe -> assertThat(recipe.reasons())
							.contains("Предложено для разнообразия меню"));
		} finally {
			cookingHistoryService.delete(recordId);
		}
	}

	@Test
	void exclusionRestrictionRemovesRecipeFromAssistant() {
		var potatoId = ingredientIds("Картофель").getFirst();
		var paprikaId = ingredientIds("Паприка").getFirst();
		var ovenId = recommendationService.getEquipmentOptions().stream()
				.filter(option -> option.name().equals("Электродуховка"))
				.findFirst()
				.orElseThrow()
				.id();
		recipeRestrictionService.delete(potatoId);

		try {
			recipeRestrictionService.save(restrictionForm(
					potatoId,
					RestrictionMode.EXCLUDE,
					"Не предлагать картофель"));

			assertThat(recommendationService.recommend(new AssistantCriteria(
					List.of(paprikaId),
					50,
					ovenId,
					null,
					Difficulty.EASY,
					AssistantGoal.BALANCED,
					false))).isEmpty();
		} finally {
			recipeRestrictionService.delete(potatoId);
		}
	}
}
