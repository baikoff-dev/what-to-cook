package io.github.baikoff.whattocook.restriction.service.model;

import io.github.baikoff.whattocook.restriction.domain.IngredientRestriction;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IngredientRestrictionForm {

	@NotNull(message = "Выбери ингредиент")
	private Long ingredientId;

	@NotNull(message = "Выбери режим ограничения")
	private RestrictionMode mode;

	@Size(max = 1000, message = "Заметка должна быть не длиннее 1000 символов")
	private String note;

	public static IngredientRestrictionForm from(IngredientRestriction restriction) {
		var form = new IngredientRestrictionForm();
		form.setIngredientId(restriction.getIngredientId());
		form.setMode(restriction.getMode());
		form.setNote(restriction.getNote());
		return form;
	}

	public Long getIngredientId() {
		return ingredientId;
	}

	public void setIngredientId(Long ingredientId) {
		this.ingredientId = ingredientId;
	}

	public RestrictionMode getMode() {
		return mode;
	}

	public void setMode(RestrictionMode mode) {
		this.mode = mode;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
