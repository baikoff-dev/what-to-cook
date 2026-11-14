package io.github.baikoff.whattocook.recipe.service;

import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.domain.CookingMethod;
import io.github.baikoff.whattocook.recipe.domain.Equipment;
import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeIngredient;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.repository.CookingMethodRepository;
import io.github.baikoff.whattocook.recipe.repository.EquipmentRepository;
import io.github.baikoff.whattocook.recipe.repository.IngredientRepository;
import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.recipe.service.model.RecipeForm;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeManagementService {

	private final RecipeRepository recipeRepository;
	private final IngredientRepository ingredientRepository;
	private final EquipmentRepository equipmentRepository;
	private final CookingMethodRepository cookingMethodRepository;
	private final RecipeImageStorage imageStorage;

	public RecipeManagementService(
			RecipeRepository recipeRepository,
			IngredientRepository ingredientRepository,
			EquipmentRepository equipmentRepository,
			CookingMethodRepository cookingMethodRepository,
			RecipeImageStorage imageStorage) {
		this.recipeRepository = recipeRepository;
		this.ingredientRepository = ingredientRepository;
		this.equipmentRepository = equipmentRepository;
		this.cookingMethodRepository = cookingMethodRepository;
		this.imageStorage = imageStorage;
	}

	@Transactional(readOnly = true)
	public RecipeForm getForm(long recipeId) {
		var recipe = findRecipe(recipeId);
		var form = new RecipeForm();
		form.setId(recipe.getId());
		form.setVersion(recipe.getVersion());
		form.setNameRu(recipe.getNameRu());
		form.setNameEn(recipe.getNameEn());
		form.setDescription(recipe.getDescription());
		form.setServings(recipe.getServings());
		form.setPreparationMinutes(recipe.getPreparationMinutes());
		form.setCookingMinutes(recipe.getCookingMinutes());
		form.setDifficulty(recipe.getDifficulty());
		form.setStatus(recipe.getStatus() == RecipeStatus.ARCHIVED ? RecipeStatus.UNTESTED : recipe.getStatus());
		form.setSourceName(recipe.getSourceName());
		form.setSourceUrl(recipe.getSourceUrl());
		form.setPersonalNotes(recipe.getPersonalNotes());
		form.setExistingMainImagePath(recipe.getMainImagePath());
		form.setEquipmentIds(recipe.getEquipment().stream().map(Equipment::getId).toList());
		form.setCookingMethodIds(recipe.getCookingMethods().stream().map(CookingMethod::getId).toList());

		form.setIngredients(recipe.getIngredients().stream().map(recipeIngredient -> {
			var row = new RecipeForm.IngredientRow();
			row.setName(recipeIngredient.getIngredient().getNameRu());
			row.setAmount(recipeIngredient.getAmount());
			row.setUnit(recipeIngredient.getUnit());
			row.setOptional(recipeIngredient.isOptional());
			return row;
		}).toList());

		form.setSteps(recipe.getSteps().stream().map(step -> {
			var row = new RecipeForm.StepRow();
			row.setId(step.getId());
			row.setInstruction(step.getInstruction());
			row.setExistingImagePath(step.getImagePath());
			return row;
		}).toList());
		return form;
	}

	@Transactional(readOnly = true)
	public List<SelectionOption> getEquipmentOptions() {
		return equipmentRepository.findAllByOrderByNameRuAsc().stream()
				.map(item -> new SelectionOption(item.getId(), item.getNameRu()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SelectionOption> getCookingMethodOptions() {
		return cookingMethodRepository.findAllByOrderByNameRuAsc().stream()
				.map(item -> new SelectionOption(item.getId(), item.getNameRu()))
				.toList();
	}

	@Transactional
	public long create(RecipeForm form) {
		var recipe = new Recipe("recipe-" + UUID.randomUUID());
		applyDetails(recipe, form);
		recipeRepository.saveAndFlush(recipe);
		replaceContent(recipe, form, Map.of());
		applyMainImage(recipe, form);
		return recipe.getId();
	}

	@Transactional
	public long update(long recipeId, RecipeForm form) {
		var recipe = findRecipe(recipeId);
		if (recipe.getVersion() != form.getVersion()) {
			throw new RecipeConflictException();
		}

		Map<Long, String> existingStepImages = new HashMap<>();
		for (var step : recipe.getSteps()) {
			existingStepImages.put(step.getId(), step.getImagePath());
		}

		applyDetails(recipe, form);
		recipe.clearIngredients();
		recipe.clearSteps();
		recipeRepository.flush();
		replaceContent(recipe, form, existingStepImages);
		applyMainImage(recipe, form);
		return recipe.getId();
	}

	@Transactional
	public void archive(long recipeId) {
		findRecipe(recipeId).archive();
	}

	@Transactional
	public void restore(long recipeId) {
		findRecipe(recipeId).restore();
	}

	private void applyDetails(Recipe recipe, RecipeForm form) {
		var equipment = equipmentRepository.findAllById(form.getEquipmentIds());
		if (equipment.size() != form.getEquipmentIds().stream().distinct().count()) {
			throw new IllegalArgumentException("Выбрано неизвестное оборудование");
		}
		var cookingMethods = cookingMethodRepository.findAllById(form.getCookingMethodIds());
		if (cookingMethods.size() != form.getCookingMethodIds().stream().distinct().count()) {
			throw new IllegalArgumentException("Выбран неизвестный способ приготовления");
		}

		recipe.updateDetails(
				normalizeRequired(form.getNameRu()),
				normalizeOptional(form.getNameEn()),
				normalizeRequired(form.getDescription()),
				form.getServings(),
				form.getPreparationMinutes(),
				form.getCookingMinutes(),
				form.getDifficulty(),
				form.getStatus(),
				normalizeOptional(form.getSourceName()),
				normalizeOptional(form.getSourceUrl()),
				normalizeOptional(form.getPersonalNotes()));
		recipe.replaceEquipment(equipment);
		recipe.replaceCookingMethods(cookingMethods);
	}

	private void replaceContent(Recipe recipe, RecipeForm form, Map<Long, String> existingStepImages) {
		for (int index = 0; index < form.getIngredients().size(); index++) {
			var row = form.getIngredients().get(index);
			var ingredientName = normalizeRequired(row.getName());
			var ingredient = ingredientRepository.findByNameRuIgnoreCase(ingredientName)
					.orElseGet(() -> ingredientRepository.save(
							new Ingredient("USER_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT),
									ingredientName)));
			recipe.addIngredient(ingredient, row.getAmount(), row.getUnit(), row.isOptional(), index + 1);
		}

		Map<Long, String> unusedStepImages = new HashMap<>(existingStepImages);
		for (int index = 0; index < form.getSteps().size(); index++) {
			var row = form.getSteps().get(index);
			String oldImagePath = null;
			if (row.getId() != null) {
				if (!existingStepImages.containsKey(row.getId())) {
					throw new IllegalArgumentException("Шаг рецепта не найден");
				}
				oldImagePath = existingStepImages.get(row.getId());
				unusedStepImages.remove(row.getId());
			}

			var imagePath = updateImage(recipe.getId(), row.getImage(), row.isRemoveImage(), oldImagePath);
			recipe.addStep(index + 1, normalizeRequired(row.getInstruction()), imagePath);
		}

		unusedStepImages.values().forEach(imageStorage::deleteAfterCommit);
	}

	private void applyMainImage(Recipe recipe, RecipeForm form) {
		var imagePath = updateImage(
				recipe.getId(),
				form.getMainImage(),
				form.isRemoveMainImage(),
				recipe.getMainImagePath());
		recipe.setMainImagePath(imagePath);
	}

	private String updateImage(long recipeId, org.springframework.web.multipart.MultipartFile newImage,
			boolean removeImage, String oldImagePath) {
		if (newImage != null && !newImage.isEmpty()) {
			var newPath = imageStorage.store(recipeId, newImage);
			imageStorage.deleteAfterCommit(oldImagePath);
			return newPath;
		}
		if (removeImage) {
			imageStorage.deleteAfterCommit(oldImagePath);
			return null;
		}
		return oldImagePath;
	}

	private Recipe findRecipe(long recipeId) {
		return recipeRepository.findById(recipeId)
				.orElseThrow(() -> new RecipeNotFoundException(recipeId));
	}

	private String normalizeRequired(String value) {
		return value.strip();
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
