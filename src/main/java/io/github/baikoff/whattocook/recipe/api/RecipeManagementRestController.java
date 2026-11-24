package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.service.RecipeManagementService;
import io.github.baikoff.whattocook.recipe.service.model.RecipeForm;
import io.github.baikoff.whattocook.recipe.web.RecipeFormValidator;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recipes")
public class RecipeManagementRestController {

	private final RecipeManagementService recipeManagementService;
	private final RecipeFormValidator recipeFormValidator;
	private final RecipeImageStorage imageStorage;

	public RecipeManagementRestController(
			RecipeManagementService recipeManagementService,
			RecipeFormValidator recipeFormValidator,
			RecipeImageStorage imageStorage) {
		this.recipeManagementService = recipeManagementService;
		this.recipeFormValidator = recipeFormValidator;
		this.imageStorage = imageStorage;
	}

	@InitBinder("recipeForm")
	void configureRecipeFormValidation(WebDataBinder binder) {
		binder.addValidators(recipeFormValidator);
	}

	@GetMapping("/form-options")
	public RecipeFormOptionsResponse getFormOptions() {
		return RecipeFormOptionsResponse.from(recipeManagementService);
	}

	@GetMapping("/{recipeId}/edit")
	public RecipeEditResponse getRecipeForEditing(@PathVariable long recipeId) {
		return RecipeEditResponse.from(recipeManagementService.getForm(recipeId), imageStorage);
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<RecipeIdResponse> create(@Valid @RequestBody RecipeForm recipeForm) {
		preserveImages(recipeForm);
		var recipeId = recipeManagementService.create(recipeForm);
		return ResponseEntity.created(URI.create("/api/recipes/" + recipeId))
				.body(new RecipeIdResponse(recipeId));
	}

	@PutMapping(value = "/{recipeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public RecipeIdResponse update(
			@PathVariable long recipeId,
			@Valid @RequestBody RecipeForm recipeForm) {
		prepareJsonImages(recipeForm);
		return new RecipeIdResponse(recipeManagementService.update(recipeId, recipeForm));
	}

	@PutMapping(value = "/{recipeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public RecipeIdResponse updateWithImages(
			@PathVariable long recipeId,
			@Valid @RequestPart("recipe") RecipeForm recipeForm,
			@RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
			@RequestPart(value = "stepImageIndexes", required = false) List<Integer> stepImageIndexes,
			@RequestPart(value = "stepImages", required = false) List<MultipartFile> stepImages) {
		prepareStepImages(recipeForm);
		recipeForm.setMainImage(mainImage);
		recipeForm.setRemoveMainImage(false);
		attachStepImages(recipeForm, stepImageIndexes, stepImages);
		return new RecipeIdResponse(recipeManagementService.update(recipeId, recipeForm));
	}

	private void attachStepImages(
			RecipeForm recipeForm,
			List<Integer> stepImageIndexes,
			List<MultipartFile> stepImages) {
		var indexes = stepImageIndexes == null ? List.<Integer>of() : stepImageIndexes;
		var images = stepImages == null ? List.<MultipartFile>of() : stepImages;
		if (indexes.size() != images.size()) {
			throw new IllegalArgumentException("Количество индексов и изображений шагов должно совпадать");
		}

		var uniqueIndexes = new HashSet<Integer>();
		for (int imageIndex = 0; imageIndex < indexes.size(); imageIndex++) {
			var stepIndex = indexes.get(imageIndex);
			if (stepIndex == null || stepIndex < 0 || stepIndex >= recipeForm.getSteps().size()) {
				throw new IllegalArgumentException("Недопустимый индекс шага для изображения");
			}
			if (!uniqueIndexes.add(stepIndex)) {
				throw new IllegalArgumentException("Для одного шага передано несколько изображений");
			}
			var step = recipeForm.getSteps().get(stepIndex);
			step.setImage(images.get(imageIndex));
			step.setRemoveImage(false);
		}
	}

	private void preserveImages(RecipeForm recipeForm) {
		prepareJsonImages(recipeForm);
		recipeForm.setRemoveMainImage(false);
		resetStepImageRemoval(recipeForm);
	}

	private void prepareJsonImages(RecipeForm recipeForm) {
		recipeForm.setMainImage(null);
		prepareStepImages(recipeForm);
	}

	private void prepareStepImages(RecipeForm recipeForm) {
		if (recipeForm.getSteps() != null) {
			recipeForm.getSteps().forEach(step -> step.setImage(null));
		}
	}

	private void resetStepImageRemoval(RecipeForm recipeForm) {
		if (recipeForm.getSteps() != null) {
			recipeForm.getSteps().forEach(step -> step.setRemoveImage(false));
		}
	}

	public record RecipeIdResponse(long id) {
	}
}
