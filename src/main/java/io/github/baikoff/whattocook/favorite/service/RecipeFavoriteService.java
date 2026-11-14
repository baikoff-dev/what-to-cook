package io.github.baikoff.whattocook.favorite.service;

import io.github.baikoff.whattocook.favorite.domain.RecipeFavorite;
import io.github.baikoff.whattocook.favorite.repository.RecipeFavoriteRepository;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.recipe.service.RecipeNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeFavoriteService {

	private final RecipeRepository recipeRepository;
	private final RecipeFavoriteRepository favoriteRepository;

	public RecipeFavoriteService(
			RecipeRepository recipeRepository,
			RecipeFavoriteRepository favoriteRepository) {
		this.recipeRepository = recipeRepository;
		this.favoriteRepository = favoriteRepository;
	}

	@Transactional
	public void add(long recipeId) {
		recipeRepository.findByIdAndStatusNot(recipeId, RecipeStatus.ARCHIVED)
				.orElseThrow(() -> new RecipeNotFoundException(recipeId));

		if (!favoriteRepository.existsById(recipeId)) {
			favoriteRepository.save(new RecipeFavorite(recipeId));
		}
	}

	@Transactional
	public void remove(long recipeId) {
		favoriteRepository.deleteById(recipeId);
	}
}
