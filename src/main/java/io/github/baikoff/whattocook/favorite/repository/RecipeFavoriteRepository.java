package io.github.baikoff.whattocook.favorite.repository;

import io.github.baikoff.whattocook.favorite.domain.RecipeFavorite;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecipeFavoriteRepository extends JpaRepository<RecipeFavorite, Long> {

	@Query("select favorite.recipeId from RecipeFavorite favorite")
	List<Long> findAllRecipeIds();
}
