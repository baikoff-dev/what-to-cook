package io.github.baikoff.whattocook.recipe.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.baikoff.whattocook.recipe.service.RecipeMatchingService;

@RestController
@RequestMapping("/api/recipe-matches")
public class RecipeMatchingRestController {

	private final RecipeMatchingService recipeMatchingService;

	public RecipeMatchingRestController(RecipeMatchingService recipeMatchingService) {
		this.recipeMatchingService = recipeMatchingService;
	}

	@GetMapping
	public RecipeMatchingResponse getMatches(
			@RequestParam(required = false) List<Long> ingredientIds) {
		return RecipeMatchingResponse.from(recipeMatchingService.match(ingredientIds));
	}
}
