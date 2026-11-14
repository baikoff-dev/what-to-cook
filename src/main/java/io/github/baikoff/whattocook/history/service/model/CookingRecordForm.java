package io.github.baikoff.whattocook.history.service.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public class CookingRecordForm {

	@NotNull(message = "Укажи дату приготовления")
	@PastOrPresent(message = "Дата приготовления не может быть в будущем")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate cookedOn = LocalDate.now();

	@Min(value = 1, message = "Оценка должна быть от 1 до 5")
	@Max(value = 5, message = "Оценка должна быть от 1 до 5")
	private Integer rating;

	@Size(max = 2000, message = "Заметка должна быть не длиннее 2000 символов")
	private String note;

	public LocalDate getCookedOn() {
		return cookedOn;
	}

	public void setCookedOn(LocalDate cookedOn) {
		this.cookedOn = cookedOn;
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
