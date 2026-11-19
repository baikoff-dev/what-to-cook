package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.recipe.service.RecipeCatalogService;
import io.github.baikoff.whattocook.recipe.service.RecipeManagementService;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.RecipeOrigin;
import io.github.baikoff.whattocook.recipe.domain.RecipeSearchCriteria;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeCatalogRestController {

	private final RecipeCatalogService recipeCatalogService;
	private final RecipeManagementService recipeManagementService;

	public RecipeCatalogRestController(
			RecipeCatalogService recipeCatalogService,
			RecipeManagementService recipeManagementService) {
		this.recipeCatalogService = recipeCatalogService;
		this.recipeManagementService = recipeManagementService;
	}

	@GetMapping
	public RecipeCatalogResponse getCatalog(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) Integer maxMinutes,
			@RequestParam(required = false) Difficulty difficulty,
			@RequestParam(required = false) Long equipmentId,
			@RequestParam(required = false) Long cookingMethodId,
			@RequestParam(required = false) Long cuisineId,
			@RequestParam(required = false) RecipeOrigin origin) {
		var criteria = new RecipeSearchCriteria(
				query,
				maxMinutes,
				difficulty,
				equipmentId,
				cookingMethodId,
				cuisineId,
				origin);
		return RecipeCatalogResponse.from(recipeCatalogService.search(criteria));
	}

	@GetMapping("/filter-options")
	public RecipeCatalogFilterOptionsResponse getFilterOptions() {
		return RecipeCatalogFilterOptionsResponse.from(recipeManagementService);
	}

	@GetMapping("/favorites")
	public RecipeCatalogResponse getFavorites() {
		return RecipeCatalogResponse.from(recipeCatalogService.getFavorites());
	}

	@GetMapping("/{id}")
	public RecipeDetailsResponse getRecipe(
			@PathVariable long id,
			@RequestParam(required = false) Integer servings) {
		return RecipeDetailsResponse.from(recipeCatalogService.getRecipe(id, servings));
	}
}
