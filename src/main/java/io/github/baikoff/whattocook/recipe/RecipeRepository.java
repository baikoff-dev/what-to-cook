package io.github.baikoff.whattocook.recipe;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

	@EntityGraph(attributePaths = {"ingredients", "ingredients.ingredient"})
	List<Recipe> findAllByStatusNotOrderByNameRuAsc(RecipeStatus status);

	List<Recipe> findAllByStatusOrderByNameRuAsc(RecipeStatus status);

	Optional<Recipe> findByIdAndStatusNot(Long id, RecipeStatus status);
}
