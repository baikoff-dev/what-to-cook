package io.github.baikoff.whattocook.restriction.api;

import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;
import io.github.baikoff.whattocook.restriction.service.model.IngredientRestrictionForm;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngredientRestrictionRequest(
		@NotNull(message = "Выбери режим ограничения") RestrictionMode mode,
		@Size(max = 1000, message = "Заметка должна быть не длиннее 1000 символов") String note) {

	IngredientRestrictionForm toForm(long ingredientId) {
		var form = new IngredientRestrictionForm();
		form.setIngredientId(ingredientId);
		form.setMode(mode);
		form.setNote(note);
		return form;
	}
}
