package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.MeasurementUnit;
import io.github.baikoff.whattocook.recipe.domain.RecipeOrigin;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.domain.SubstitutionType;
import io.github.baikoff.whattocook.recipe.service.model.RecipeForm;

import java.math.BigDecimal;
import java.util.List;

public record RecipeEditResponse(
		Long id,
		long version,
		String nameRu,
		String nameEn,
		String description,
		int servings,
		int preparationMinutes,
		int cookingMinutes,
		Difficulty difficulty,
		RecipeStatus status,
		Long cuisineId,
		RecipeOrigin origin,
		String sourceName,
		String sourceUrl,
		String videoUrl,
		String personalNotes,
		List<Long> equipmentIds,
		List<Long> cookingMethodIds,
		List<IngredientRow> ingredients,
		List<SubstitutionRow> substitutions,
		List<StepRow> steps,
		String mainImageUrl) {

	public RecipeEditResponse {
		equipmentIds = List.copyOf(equipmentIds);
		cookingMethodIds = List.copyOf(cookingMethodIds);
		ingredients = List.copyOf(ingredients);
		substitutions = List.copyOf(substitutions);
		steps = List.copyOf(steps);
	}

	static RecipeEditResponse from(RecipeForm form, RecipeImageStorage imageStorage) {
		return new RecipeEditResponse(
				form.getId(),
				form.getVersion(),
				form.getNameRu(),
				form.getNameEn(),
				form.getDescription(),
				form.getServings(),
				form.getPreparationMinutes(),
				form.getCookingMinutes(),
				form.getDifficulty(),
				form.getStatus(),
				form.getCuisineId(),
				form.getOrigin(),
				form.getSourceName(),
				form.getSourceUrl(),
				form.getVideoUrl(),
				form.getPersonalNotes(),
				form.getEquipmentIds(),
				form.getCookingMethodIds(),
				form.getIngredients().stream().map(IngredientRow::from).toList(),
				form.getSubstitutions().stream().map(SubstitutionRow::from).toList(),
				form.getSteps().stream().map(step -> StepRow.from(step, imageStorage)).toList(),
				imageStorage.publicUrl(form.getExistingMainImagePath()));
	}

	public record IngredientRow(
			String name,
			BigDecimal amount,
			MeasurementUnit unit,
			boolean optional) {

		private static IngredientRow from(RecipeForm.IngredientRow row) {
			return new IngredientRow(row.getName(), row.getAmount(), row.getUnit(), row.isOptional());
		}
	}

	public record SubstitutionRow(
			String originalIngredientName,
			String substituteIngredientName,
			BigDecimal amount,
			MeasurementUnit unit,
			SubstitutionType type,
			String impactNote) {

		private static SubstitutionRow from(RecipeForm.SubstitutionRow row) {
			return new SubstitutionRow(
					row.getOriginalIngredientName(),
					row.getSubstituteIngredientName(),
					row.getAmount(),
					row.getUnit(),
					row.getType(),
					row.getImpactNote());
		}
	}

	public record StepRow(Long id, String instruction, String imageUrl) {

		private static StepRow from(RecipeForm.StepRow row, RecipeImageStorage imageStorage) {
			return new StepRow(
					row.getId(),
					row.getInstruction(),
					imageStorage.publicUrl(row.getExistingImagePath()));
		}
	}
}
