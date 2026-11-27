package io.github.baikoff.whattocook.test.acceptance.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.jackson.databind.ObjectMapper;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;
import io.github.baikoff.whattocook.test.support.AbstractIntegrationTest;

class AssistantRecommendationRestAcceptanceTest extends AbstractIntegrationTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private DishRecommendationService recommendationService;

	@Test
	void apiReturnsUpToThreeSurpriseRecommendationsForFrontend() throws IOException, InterruptedException {
		var response = postJson("/api/assistant/recommendations", "{\"mode\":\"SURPRISE\"}");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type").orElse(""))
				.startsWith("application/json");

		var items = objectMapper.readTree(response.body()).path("items");
		assertThat(items.isArray()).isTrue();
		assertThat(items).hasSize(3);
		assertThat(items).allSatisfy(item -> {
			assertThat(item.path("id").isNumber()).isTrue();
			assertThat(item.path("nameRu").asText()).isNotBlank();
			assertThat(item.path("description").asText()).isNotBlank();
			assertThat(item.has("coverUrl")).isTrue();
			assertThat(item.path("totalMinutes").asInt()).isPositive();
			assertThat(item.path("difficulty").asText()).isNotBlank();
		});
	}

	@Test
	void apiReturnsAssistantOptionsForFrontend() throws IOException, InterruptedException {
		var response = get("/api/assistant/options");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type").orElse(""))
				.startsWith("application/json");
		assertThat(response.body())
				.contains("\"ingredients\"")
				.contains("\"name\":\"Паприка\"")
				.contains("\"equipment\"")
				.contains("\"name\":\"Электродуховка\"")
				.contains("\"cookingMethods\"")
				.contains("\"name\":\"Запекание\"")
				.contains("\"difficulties\"")
				.contains("\"value\":\"EASY\",\"name\":\"Легко\"")
				.contains("\"goals\"")
				.contains("\"value\":\"USE_PRODUCTS\",\"name\":\"Использовать имеющиеся продукты\"");
	}

	@Test
	void apiUsesProductsAndPreferencesForRecommendations() throws IOException, InterruptedException {
		var paprikaId = ingredientIds("Паприка").getFirst();
		var potatoId = ingredientIds("Картофель").getFirst();
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
		var body = """
				{
				  "mode": "PREFERENCES",
				  "ingredientIds": [%d],
				  "maxMinutes": 50,
				  "equipmentId": %d,
				  "cookingMethodId": %d,
				  "maxDifficulty": "EASY",
				  "goal": "USE_PRODUCTS"
				}
				""".formatted(paprikaId, ovenId, bakingId);

		recipeRestrictionService.delete(potatoId);
		try {
			recipeRestrictionService.save(restrictionForm(
					potatoId,
					RestrictionMode.WARN,
					"Можно редко"));
			var response = postJson("/api/assistant/recommendations", body);

			assertThat(response.statusCode()).isEqualTo(200);
			var items = objectMapper.readTree(response.body()).path("items");
			assertThat(items).hasSize(1);
			var item = items.get(0);
			assertThat(item.path("nameRu").asText())
					.isEqualTo("Картофель, запечённый в духовке");
			assertThat(item.path("equipment").toString()).contains("Электродуховка");
			assertThat(item.path("cookingMethods").toString()).contains("Запекание");
			assertThat(item.path("ingredientsSpecified").asBoolean()).isTrue();
			assertThat(item.path("matchPercentage").asInt()).isEqualTo(60);
			assertThat(item.path("matchedIngredientCount").asInt()).isEqualTo(3);
			assertThat(item.path("requiredIngredientCount").asInt()).isEqualTo(5);
			assertThat(item.path("missingIngredients").toString())
					.contains("Картофель", "Чеснок");
			assertThat(item.path("pantryIngredientNames").toString())
					.contains("Растительное масло", "Соль");
			assertThat(item.has("lastCookedOn")).isTrue();
			assertThat(item.path("favorite").asBoolean()).isFalse();
			assertThat(item.has("averageRating")).isTrue();
			assertThat(item.path("reasons").toString())
					.contains("Есть 3 из 5 обязательных продуктов")
					.contains("Подходит для оборудования: Электродуховка");
			assertThat(item.path("restrictionWarnings").toString())
					.contains("Картофель", "Предупреждать", "Можно редко");
		} finally {
			recipeRestrictionService.delete(potatoId);
		}
	}

	@Test
	void apiReturnsUsedIngredientSubstitutionDetails() throws IOException, InterruptedException {
		var drySherryId = ingredientIds("Сухой херес").getFirst();
		var body = """
				{
				  "mode": "PREFERENCES",
				  "ingredientIds": [%d],
				  "goal": "USE_PRODUCTS"
				}
				""".formatted(drySherryId);

		var response = postJson("/api/assistant/recommendations", body);

		assertThat(response.statusCode()).isEqualTo(200);
		var items = objectMapper.readTree(response.body()).path("items");
		var recipe = items.valueStream()
				.filter(item -> item.path("nameRu").asText().equals("Помидоры с яйцом по-китайски"))
				.findFirst()
				.orElseThrow();
		assertThat(recipe.path("substitutionUsages").toString())
				.contains("Шаосинское вино", "Сухой херес")
				.contains("substitutionType", "impactNote");
	}

	@Test
	void apiRejectsInvalidPreferenceCriteria() throws IOException, InterruptedException {
		var missingProducts = postJson("/api/assistant/recommendations", """
				{
				  "mode": "PREFERENCES",
				  "ingredientIds": [],
				  "goal": "USE_PRODUCTS"
				}
				""");
		var excessiveTime = postJson("/api/assistant/recommendations", """
				{
				  "mode": "PREFERENCES",
				  "ingredientIds": [],
				  "maxMinutes": 1441,
				  "goal": "BALANCED"
				}
				""");

		assertThat(missingProducts.statusCode()).isEqualTo(400);
		assertThat(excessiveTime.statusCode()).isEqualTo(400);
	}

	private HttpResponse<String> postJson(String path, String body) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}
}
