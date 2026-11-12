package io.github.baikoff.whattocook.recipe;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecipeRepository extends JpaRepository<Recipe, Long> {

	List<Recipe> findAllByStatusNotOrderByNameRuAsc(RecipeStatus status);

	List<Recipe> findAllByStatusOrderByNameRuAsc(RecipeStatus status);

	Optional<Recipe> findByIdAndStatusNot(Long id, RecipeStatus status);
}
