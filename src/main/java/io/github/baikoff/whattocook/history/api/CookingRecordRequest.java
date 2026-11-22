package io.github.baikoff.whattocook.history.api;

import java.time.LocalDate;

import io.github.baikoff.whattocook.history.service.model.CookingRecordForm;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record CookingRecordRequest(
		@NotNull(message = "Укажи дату приготовления")
		@PastOrPresent(message = "Дата приготовления не может быть в будущем")
		LocalDate cookedOn,
		@Min(value = 1, message = "Оценка должна быть от 1 до 5")
		@Max(value = 5, message = "Оценка должна быть от 1 до 5")
		Integer rating,
		@Size(max = 2000, message = "Заметка должна быть не длиннее 2000 символов")
		String note) {

	CookingRecordForm toForm() {
		var form = new CookingRecordForm();
		form.setCookedOn(cookedOn);
		form.setRating(rating);
		form.setNote(note);
		return form;
	}
}
