package io.github.baikoff.whattocook.history.service.model;

import java.time.LocalDate;

public record CookingHistoryItem(
		Long id,
		Long recipeId,
		String recipeName,
		LocalDate cookedOn,
		Integer rating,
		String note,
		boolean archived) {
}
