package io.github.baikoff.whattocook.favorite.api;

import io.github.baikoff.whattocook.favorite.service.RecipeFavoriteService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes/{recipeId}/favorite")
public class RecipeFavoriteRestController {

	private final RecipeFavoriteService recipeFavoriteService;

	public RecipeFavoriteRestController(RecipeFavoriteService recipeFavoriteService) {
		this.recipeFavoriteService = recipeFavoriteService;
	}

	@PutMapping
	public RecipeFavoriteResponse add(@PathVariable long recipeId) {
		recipeFavoriteService.add(recipeId);
		return new RecipeFavoriteResponse(true);
	}

	@DeleteMapping
	public RecipeFavoriteResponse remove(@PathVariable long recipeId) {
		recipeFavoriteService.remove(recipeId);
		return new RecipeFavoriteResponse(false);
	}

	public record RecipeFavoriteResponse(boolean favorite) {
	}
}
