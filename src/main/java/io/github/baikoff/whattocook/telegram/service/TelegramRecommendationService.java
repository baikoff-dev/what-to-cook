package io.github.baikoff.whattocook.telegram.service;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.assistant.service.model.AssistantCriteria;
import io.github.baikoff.whattocook.assistant.service.model.AssistantGoal;
import io.github.baikoff.whattocook.assistant.service.model.DishRecommendation;
import io.github.baikoff.whattocook.image.storage.RecipeImageContentReader;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;
import io.github.baikoff.whattocook.telegram.model.TelegramButton;
import io.github.baikoff.whattocook.telegram.model.TelegramOutgoingMessage;
import io.github.baikoff.whattocook.telegram.model.TelegramPhoto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TelegramRecommendationService {

	static final String SURPRISE_CALLBACK = "SURPRISE";
	static final String SELECT_PRODUCTS_CALLBACK = "SELECT_PRODUCTS";
	static final String NEXT_PRODUCT_CALLBACK = "NEXT_PRODUCT";
	static final String CHANGE_PRODUCTS_CALLBACK = "CHANGE_PRODUCTS";
	static final String MENU_CALLBACK = "MENU";

	private static final TelegramButton REPEAT_BUTTON =
			new TelegramButton("Удиви ещё раз", SURPRISE_CALLBACK);
	private static final List<TelegramButton> PRODUCT_RESULT_BUTTONS = List.of(
			new TelegramButton("Другой вариант", NEXT_PRODUCT_CALLBACK),
			new TelegramButton("Изменить продукты", CHANGE_PRODUCTS_CALLBACK));
	private static final TelegramButton CANCEL_BUTTON =
			new TelegramButton("Вернуться в меню", MENU_CALLBACK);
	private static final Map<String, String> PRODUCT_ALIASES = Map.of(
			"лук", "репчатый лук");

	private final DishRecommendationService recommendationService;
	private final RecipeImageContentReader imageContentReader;
	private final ConcurrentMap<Long, ProductSelectionSession> productSessions =
			new ConcurrentHashMap<>();

	public TelegramRecommendationService(
			DishRecommendationService recommendationService,
			RecipeImageContentReader imageContentReader) {
		this.recommendationService = recommendationService;
		this.imageContentReader = imageContentReader;
	}

	public TelegramOutgoingMessage surprise(long chatId) {
		return recommendationService.recommend(AssistantCriteria.surpriseMe()).stream()
				.findFirst()
				.map(recommendation -> recipeMessage(chatId, recommendation, List.of(REPEAT_BUTTON)))
				.orElseGet(() -> emptyMessage(chatId, List.of(REPEAT_BUTTON)));
	}

	public TelegramOutgoingMessage beginProductSelection(long chatId) {
		productSessions.put(chatId, ProductSelectionSession.waitingForInput());
		return new TelegramOutgoingMessage(
				chatId,
				"Напиши продукты через запятую. Например: картофель, чеснок, лук.",
				List.of(CANCEL_BUTTON));
	}

	public boolean awaitsProductInput(long chatId) {
		var session = productSessions.get(chatId);
		return session != null && session.awaitingInput();
	}

	public TelegramOutgoingMessage recommendByProducts(long chatId, String productNames) {
		var resolution = resolveProducts(productNames);
		if (!resolution.unknownNames().isEmpty()) {
			productSessions.put(chatId, ProductSelectionSession.waitingForInput());
			return unknownProductsMessage(chatId, resolution.unknownNames());
		}
		if (resolution.ingredientIds().isEmpty()) {
			productSessions.put(chatId, ProductSelectionSession.waitingForInput());
			return new TelegramOutgoingMessage(
					chatId,
					"Не получилось распознать продукты. Напиши хотя бы один продукт из справочника.",
					List.of(CANCEL_BUTTON));
		}

		var criteria = new AssistantCriteria(
				resolution.ingredientIds(),
				null,
				null,
				null,
				null,
				AssistantGoal.USE_PRODUCTS,
				false);
		var recommendations = recommendationService.recommend(criteria);
		if (recommendations.isEmpty()) {
			productSessions.put(chatId, ProductSelectionSession.withRecommendations(List.of(), 0));
			return emptyMessage(
					chatId,
					List.of(new TelegramButton("Изменить продукты", CHANGE_PRODUCTS_CALLBACK)));
		}

		productSessions.put(chatId, ProductSelectionSession.withRecommendations(recommendations, 0));
		return recipeMessage(chatId, recommendations.getFirst(), PRODUCT_RESULT_BUTTONS);
	}

	public TelegramOutgoingMessage nextProductRecommendation(long chatId) {
		var session = productSessions.get(chatId);
		if (session == null || session.recommendations().isEmpty()) {
			return beginProductSelection(chatId);
		}
		var nextIndex = (session.currentIndex() + 1) % session.recommendations().size();
		productSessions.put(
				chatId,
				ProductSelectionSession.withRecommendations(session.recommendations(), nextIndex));
		return recipeMessage(
				chatId,
				session.recommendations().get(nextIndex),
				PRODUCT_RESULT_BUTTONS);
	}

	public void cancelProductSelection(long chatId) {
		productSessions.remove(chatId);
	}

	private ProductResolution resolveProducts(String productNames) {
		var optionsByName = recommendationService.getIngredientOptions().stream()
				.collect(Collectors.toMap(
						option -> normalize(option.name()),
						Function.identity(),
						(first, ignored) -> first));
		var requestedNames = productNames == null
				? List.<String>of()
				: Arrays.stream(productNames.split(","))
						.map(String::strip)
						.filter(name -> !name.isBlank())
						.toList();
		var ingredientIds = new LinkedHashSet<Long>();
		var unknownNames = new ArrayList<String>();
		for (var requestedName : requestedNames) {
			var normalizedName = normalize(requestedName);
			var canonicalName = PRODUCT_ALIASES.getOrDefault(normalizedName, normalizedName);
			var option = optionsByName.get(canonicalName);
			if (option == null) {
				unknownNames.add(requestedName);
			} else {
				ingredientIds.add(option.id());
			}
		}
		return new ProductResolution(List.copyOf(ingredientIds), List.copyOf(unknownNames));
	}

	private String normalize(String value) {
		return value.strip()
				.toLowerCase(Locale.forLanguageTag("ru"))
				.replace('ё', 'е')
				.replaceAll("\\s+", " ");
	}

	private TelegramOutgoingMessage unknownProductsMessage(long chatId, List<String> unknownNames) {
		return new TelegramOutgoingMessage(
				chatId,
				"Не нашёл в справочнике: %s. Проверь названия и отправь весь список ещё раз."
						.formatted(String.join(", ", unknownNames)),
				List.of(CANCEL_BUTTON));
	}

	private TelegramOutgoingMessage recipeMessage(
			long chatId,
			DishRecommendation recommendation,
			List<TelegramButton> buttons) {
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

		var photo = imageContentReader.read(recommendation.mainImageUrl())
				.map(image -> new TelegramPhoto(
						image.fileName(),
						image.mediaType(),
						image.bytes()))
				.orElse(null);
		return new TelegramOutgoingMessage(chatId, text.toString(), buttons, photo);
	}

	private String ingredientsSummary(DishRecommendation recommendation) {
		if (!recommendation.ingredientsSpecified()) {
			return "";
		}
		var match = "Совпадение: %d из %d (%d%%)".formatted(
				recommendation.matchedIngredientCount(),
				recommendation.requiredIngredientCount(),
				recommendation.matchPercentage());
		if (recommendation.missingIngredients().isEmpty()) {
			return match + "\nПродукты: всё необходимое есть";
		}
		return match + "\nНе хватает: " + String.join(", ", recommendation.missingIngredients());
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

	private TelegramOutgoingMessage emptyMessage(long chatId, List<TelegramButton> buttons) {
		return new TelegramOutgoingMessage(
				chatId,
				"Пока не нашёл подходящий рецепт. Проверь ограничения или попробуй ещё раз.",
				buttons);
	}

	private record ProductResolution(List<Long> ingredientIds, List<String> unknownNames) {
	}

	private record ProductSelectionSession(
			boolean awaitingInput,
			List<DishRecommendation> recommendations,
			int currentIndex) {

		private ProductSelectionSession {
			recommendations = List.copyOf(recommendations);
		}

		private static ProductSelectionSession waitingForInput() {
			return new ProductSelectionSession(true, List.of(), 0);
		}

		private static ProductSelectionSession withRecommendations(
				List<DishRecommendation> recommendations,
				int currentIndex) {
			return new ProductSelectionSession(false, recommendations, currentIndex);
		}
	}
}
