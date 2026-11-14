package io.github.baikoff.whattocook.recipe.repository;

import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

	@EntityGraph(attributePaths = {"ingredients", "ingredients.ingredient"})
	List<Recipe> findAllByStatusNotOrderByNameRuAsc(RecipeStatus status);

	List<Recipe> findAllByStatusOrderByNameRuAsc(RecipeStatus status);

	@EntityGraph(attributePaths = {"ingredients", "ingredients.ingredient"})
	List<Recipe> findAllByIdInAndStatusNotOrderByNameRuAsc(List<Long> ids, RecipeStatus status);

	Optional<Recipe> findByIdAndStatusNot(Long id, RecipeStatus status);
}
