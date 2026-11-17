package io.github.baikoff.whattocook.test.acceptance.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class IngredientSubstitutionMatchingAcceptanceTest extends AbstractIntegrationTest {

    private static final String RECIPE_NAME = "Помидоры с яйцом по-китайски";
    private static final String SUBSTITUTION_EXPLANATION =
            "Учтена замена: Шаосинское вино → Сухой херес";

    @Test
    @DisplayName("Подбор по продуктам учитывает допустимую замену ингредиента")
    void matchingPageCountsAllowedSubstitution() throws IOException, InterruptedException, URISyntaxException {
        var drySherryId = ingredientIds("Сухой херес").getFirst();

        var response = get("/recipes/match?ingredientIds=" + drySherryId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertSubstitutionMatch(response.body());
    }

    @Test
    @DisplayName("Помощник учитывает допустимую замену ингредиента")
    void assistantCountsAllowedSubstitution() throws IOException, InterruptedException, URISyntaxException {
        var drySherryId = ingredientIds("Сухой херес").getFirst();

        var response = get("/assistant?ingredientIds=" + drySherryId
                + "&goal=USE_PRODUCTS&mode=PREFERENCES");

        assertThat(response.statusCode()).isEqualTo(200);
        assertSubstitutionMatch(response.body());
    }

    private void assertSubstitutionMatch(String responseBody) {
        assertThat(responseBody)
                .contains(RECIPE_NAME)
                .contains("40%")
                .contains("Есть 4 из 10 обязательных продуктов")
                .contains(SUBSTITUTION_EXPLANATION);
    }
}
