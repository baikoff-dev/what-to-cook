package io.github.baikoff.whattocook.recipe.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RecipeArchiveStatusTest {

	@ParameterizedTest
	@EnumSource(value = RecipeStatus.class, names = {"UNTESTED", "TESTED", "NEEDS_CHANGES"})
	void archiveAndRestorePreserveWorkingStatus(RecipeStatus workingStatus) {
		var recipe = recipeWithStatus(workingStatus);

		recipe.archive();
		recipe.archive();

		assertThat(recipe.getStatus()).isEqualTo(RecipeStatus.ARCHIVED);

		recipe.restore();
		recipe.restore();

		assertThat(recipe.getStatus()).isEqualTo(workingStatus);
		assertThat(recipe.getStatusBeforeArchive()).isNull();
	}

	private Recipe recipeWithStatus(RecipeStatus status) {
		var recipe = new Recipe("archive-status-test");
		recipe.updateDetails(
				"Тестовый рецепт",
				null,
				"Проверка переходов между статусами",
				1,
				0,
				1,
				Difficulty.EASY,
				status,
				null,
				RecipeOrigin.PERSONAL,
				null,
				null,
				null,
				null);
		return recipe;
	}
}
