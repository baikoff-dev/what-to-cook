package io.github.baikoff.whattocook.recipe.web;

import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.service.model.RecipeForm;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class RecipeFormValidator implements Validator {

	private final RecipeImageStorage imageStorage;

	public RecipeFormValidator(RecipeImageStorage imageStorage) {
		this.imageStorage = imageStorage;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return RecipeForm.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		var form = (RecipeForm) target;

		if (form.getStatus() == RecipeStatus.ARCHIVED) {
			errors.rejectValue("status", "recipe.status.archived", "Для архива используй отдельную кнопку");
		}

		Set<String> ingredientNames = new HashSet<>();
		if (form.getIngredients() != null) {
			for (int index = 0; index < form.getIngredients().size(); index++) {
				var name = form.getIngredients().get(index).getName();
				if (name != null && !name.isBlank()) {
					var normalized = name.strip().toLowerCase(Locale.ROOT);
					if (!ingredientNames.add(normalized)) {
						errors.rejectValue("ingredients[" + index + "].name", "recipe.ingredient.duplicate",
								"Ингредиент уже добавлен в рецепт");
					}
				}
			}
		}

		validateImage(form.getMainImage(), "mainImage", errors);
		if (form.getSteps() != null) {
			for (int index = 0; index < form.getSteps().size(); index++) {
				validateImage(form.getSteps().get(index).getImage(), "steps[" + index + "].image", errors);
			}
		}
	}

	private void validateImage(org.springframework.web.multipart.MultipartFile file, String field, Errors errors) {
		imageStorage.validationError(file)
				.ifPresent(message -> errors.rejectValue(field, "recipe.image.invalid", message));
	}
}
