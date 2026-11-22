package io.github.baikoff.whattocook.history.api;

import java.time.LocalDate;
import java.util.List;

import io.github.baikoff.whattocook.history.service.model.CookingHistoryItem;

public record CookingHistoryResponse(
		List<CookingHistoryItemResponse> items,
		int total) {

	static CookingHistoryResponse from(List<CookingHistoryItem> history) {
		return new CookingHistoryResponse(
				history.stream().map(CookingHistoryItemResponse::from).toList(),
				history.size());
	}

	public record CookingHistoryItemResponse(
			Long id,
			Long recipeId,
			String recipeName,
			LocalDate cookedOn,
			Integer rating,
			String note,
			boolean archived) {

		static CookingHistoryItemResponse from(CookingHistoryItem item) {
			return new CookingHistoryItemResponse(
					item.id(),
					item.recipeId(),
					item.recipeName(),
					item.cookedOn(),
					item.rating(),
					item.note(),
					item.archived());
		}
	}
}
