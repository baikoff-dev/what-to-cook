package io.github.baikoff.whattocook.recipe.repository;

import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.Recipe;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

	List<Ingredient> findAllByOrderByNameRuAsc();

	Optional<Ingredient> findByNameRuIgnoreCase(String nameRu);
}
