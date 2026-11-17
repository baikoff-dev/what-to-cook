package io.github.baikoff.whattocook.telegram.service;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.assistant.service.model.AssistantCriteria;
import io.github.baikoff.whattocook.assistant.service.model.DishRecommendation;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;
import io.github.baikoff.whattocook.telegram.model.TelegramButton;
import io.github.baikoff.whattocook.telegram.model.TelegramOutgoingMessage;
import java.util.List;

public class TelegramRecommendationService {

	private static final TelegramButton REPEAT_BUTTON =
			new TelegramButton("Удиви ещё раз", "SURPRISE");

	private final DishRecommendationService recommendationService;

	public TelegramRecommendationService(DishRecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	public TelegramOutgoingMessage surprise(long chatId) {
		return recommendationService.recommend(AssistantCriteria.surpriseMe()).stream()
				.findFirst()
				.map(recommendation -> recipeMessage(chatId, recommendation))
				.orElseGet(() -> emptyMessage(chatId));
	}

	private TelegramOutgoingMessage recipeMessage(long chatId, DishRecommendation recommendation) {
		var text = new StringBuilder()
				.append("🍽 ")
				.append(recommendation.nameRu())
				.append("\n\n");
		if (recommendation.description() != null && !recommendation.description().isBlank()) {
			text.append(recommendation.description().strip()).append("\n\n");
		}
		text.append("⏱ ")
				.append(recommendation.totalMinutes())
				.append(" мин\n")
				.append("Сложность: ")
				.append(recommendation.difficulty());

		var ingredientsSummary = ingredientsSummary(recommendation);
		if (!ingredientsSummary.isBlank()) {
			text.append("\n").append(ingredientsSummary);
		}

		var restrictionSummary = restrictionSummary(recommendation.restrictionWarnings());
		if (!restrictionSummary.isBlank()) {
			text.append("\n").append(restrictionSummary);
		}

		return new TelegramOutgoingMessage(chatId, text.toString(), List.of(REPEAT_BUTTON));
	}

	private String ingredientsSummary(DishRecommendation recommendation) {
		if (!recommendation.ingredientsSpecified()) {
			return "";
		}
		if (recommendation.missingIngredients().isEmpty()) {
			return "Продукты: всё необходимое есть";
		}
		return "Не хватает: " + String.join(", ", recommendation.missingIngredients());
	}

	private String restrictionSummary(List<RestrictionWarning> warnings) {
		if (warnings.isEmpty()) {
			return "";
		}
		var items = warnings.stream()
				.map(this::restrictionItem)
				.toList();
		return "⚠ Ограничения: " + String.join("; ", items);
	}

	private String restrictionItem(RestrictionWarning warning) {
		if (warning.note() == null || warning.note().isBlank()) {
			return warning.ingredientName();
		}
		return warning.ingredientName() + " — " + warning.note().strip();
	}

	private TelegramOutgoingMessage emptyMessage(long chatId) {
		return new TelegramOutgoingMessage(
				chatId,
				"Пока не нашёл подходящий рецепт. Проверь ограничения или попробуй ещё раз.",
				List.of(REPEAT_BUTTON));
	}
}
