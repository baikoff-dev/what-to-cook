package io.github.baikoff.whattocook.test.acceptance.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class RecipeStatusRestorationAcceptanceTest extends AbstractIntegrationTest {

	@Autowired
	private RecipeRepository recipeRepository;

	@Test
	@DisplayName("После восстановления из архива рецепт сохраняет прежний статус")
	void restoredRecipeKeepsItsPreviousStatus() throws IOException, InterruptedException {
		var form = validRecipeForm();
		form.setNameRu("Рецепт для восстановления статуса");
		form.setStatus(RecipeStatus.TESTED);
		var recipeId = recipeManagementService.create(form);

		try {
			var archiveResponse = postForm("/recipes/" + recipeId + "/archive", "");
			var restoreResponse = postForm("/recipes/" + recipeId + "/restore", "");
			var detailsResponse = get("/recipes/" + recipeId);

			assertThat(archiveResponse.statusCode()).isEqualTo(302);
			assertThat(restoreResponse.statusCode()).isEqualTo(302);
			assertThat(detailsResponse.statusCode()).isEqualTo(200);
			assertThat(detailsResponse.body())
					.contains("Проверен")
					.doesNotContain("Не проверен");
		} finally {
			recipeRepository.deleteById(recipeId);
		}
	}
}
