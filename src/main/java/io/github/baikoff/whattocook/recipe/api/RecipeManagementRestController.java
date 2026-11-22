package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.image.storage.RecipeImageStorage;
import io.github.baikoff.whattocook.recipe.service.RecipeManagementService;
import io.github.baikoff.whattocook.recipe.service.model.RecipeForm;
import io.github.baikoff.whattocook.recipe.web.RecipeFormValidator;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping
	public ResponseEntity<RecipeIdResponse> create(@Valid @RequestBody RecipeForm recipeForm) {
		preserveImages(recipeForm);
		var recipeId = recipeManagementService.create(recipeForm);
		return ResponseEntity.created(URI.create("/api/recipes/" + recipeId))
				.body(new RecipeIdResponse(recipeId));
	}

	@PutMapping("/{recipeId}")
	public RecipeIdResponse update(
			@PathVariable long recipeId,
			@Valid @RequestBody RecipeForm recipeForm) {
		preserveImages(recipeForm);
		return new RecipeIdResponse(recipeManagementService.update(recipeId, recipeForm));
	}

	private void preserveImages(RecipeForm recipeForm) {
		recipeForm.setMainImage(null);
		recipeForm.setRemoveMainImage(false);
		if (recipeForm.getSteps() != null) {
			recipeForm.getSteps().forEach(step -> {
				step.setImage(null);
				step.setRemoveImage(false);
			});
		}
	}

	public record RecipeIdResponse(long id) {
	}
}
