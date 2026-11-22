package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.recipe.service.RecipeManagementService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes/{recipeId}/archive")
public class RecipeArchiveRestController {

	private final RecipeManagementService recipeManagementService;

	public RecipeArchiveRestController(RecipeManagementService recipeManagementService) {
		this.recipeManagementService = recipeManagementService;
	}

	@PutMapping
	public RecipeArchiveResponse archive(@PathVariable long recipeId) {
		recipeManagementService.archive(recipeId);
		return new RecipeArchiveResponse(true);
	}

	@DeleteMapping
	public RecipeArchiveResponse restore(@PathVariable long recipeId) {
		recipeManagementService.restore(recipeId);
		return new RecipeArchiveResponse(false);
	}

	public record RecipeArchiveResponse(boolean archived) {
	}
}
