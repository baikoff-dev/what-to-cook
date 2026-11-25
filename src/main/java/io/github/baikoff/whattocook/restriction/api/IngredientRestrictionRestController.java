package io.github.baikoff.whattocook.restriction.api;

import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionService;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ingredient-restrictions")
public class IngredientRestrictionRestController {

	private final RecipeRestrictionService restrictionService;

	public IngredientRestrictionRestController(RecipeRestrictionService restrictionService) {
		this.restrictionService = restrictionService;
	}

	@GetMapping
	public IngredientRestrictionPageResponse getPage() {
		return IngredientRestrictionPageResponse.from(restrictionService);
	}

	@PutMapping("/{ingredientId}")
	public ResponseEntity<Void> save(
			@PathVariable long ingredientId,
			@Valid @RequestBody IngredientRestrictionRequest request) {
		var created = restrictionService.save(request.toForm(ingredientId));
		if (created) {
			return ResponseEntity.created(URI.create(
					"/api/ingredient-restrictions/" + ingredientId)).build();
		}
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{ingredientId}")
	public ResponseEntity<Void> delete(@PathVariable long ingredientId) {
		restrictionService.delete(ingredientId);
		return ResponseEntity.noContent().build();
	}
}
