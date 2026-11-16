package io.github.baikoff.whattocook.test.acceptance.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeVideoLinkAcceptanceTest extends AbstractIntegrationTest {

	private static final String VIDEO_URL = "https://video.example/recipes/test-soup";

	@Autowired
	private RecipeRepository recipeRepository;

	@Test
	@DisplayName("После создания рецепта ссылка на видео отображается в его карточке")
	void createdRecipeShowsVideoLink() throws IOException, InterruptedException {
		var cuisineId = recipeManagementService.getCuisineOptions().getFirst().id();
		var equipmentId = recipeManagementService.getEquipmentOptions().getFirst().id();
		var cookingMethodId = recipeManagementService.getCookingMethodOptions().getFirst().id();
		Long createdRecipeId = null;

		try {
			var fields = new ArrayList<Map.Entry<String, String>>();
			fields.add(Map.entry("nameRu", "Тестовый суп с видео"));
			fields.add(Map.entry("description", "Рецепт для проверки ссылки на видео"));
			fields.add(Map.entry("servings", "2"));
			fields.add(Map.entry("preparationMinutes", "10"));
			fields.add(Map.entry("cookingMinutes", "20"));
			fields.add(Map.entry("difficulty", "EASY"));
			fields.add(Map.entry("status", "UNTESTED"));
			fields.add(Map.entry("cuisineId", cuisineId.toString()));
			fields.add(Map.entry("origin", "PERSONAL"));
			fields.add(Map.entry("equipmentIds", equipmentId.toString()));
			fields.add(Map.entry("cookingMethodIds", cookingMethodId.toString()));
			fields.add(Map.entry("steps[0].instruction", "Приготовить блюдо и свериться с видео."));
			fields.add(Map.entry("videoUrl", VIDEO_URL));

			for (int index = 0; index < 10; index++) {
				fields.add(Map.entry("ingredients[" + index + "].name", "Тестовый продукт " + index));
				fields.add(Map.entry("ingredients[" + index + "].amount", "100"));
				fields.add(Map.entry("ingredients[" + index + "].unit", "GRAM"));
				fields.add(Map.entry("_ingredients[" + index + "].optional", "on"));
			}

			var createResponse = postMultipartForm("/recipes/new", fields);

			assertThat(createResponse.statusCode()).isEqualTo(302);
			var location = createResponse.headers().firstValue("Location").orElseThrow();
			createdRecipeId = recipeIdFrom(location);

			var detailsResponse = get("/recipes/" + createdRecipeId);

			assertThat(detailsResponse.statusCode()).isEqualTo(200);
			assertThat(detailsResponse.body())
					.contains("Посмотреть видео приготовления")
					.contains("href=\"" + VIDEO_URL + "\"")
					.contains("target=\"_blank\"")
					.contains("rel=\"noopener noreferrer\"");
		} finally {
			if (createdRecipeId != null && recipeRepository.existsById(createdRecipeId)) {
				recipeRepository.deleteById(createdRecipeId);
			}
		}
	}

	private long recipeIdFrom(String location) {
		var idSegment = location.substring(location.lastIndexOf('/') + 1);
		var pathParameterStart = idSegment.indexOf(';');
		if (pathParameterStart >= 0) {
			idSegment = idSegment.substring(0, pathParameterStart);
		}
		return Long.parseLong(idSegment);
	}
}
