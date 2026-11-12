package io.github.baikoff.whattocook.recipe;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface IngredientRepository extends JpaRepository<Ingredient, Long> {

	List<Ingredient> findAllByOrderByNameRuAsc();

	Optional<Ingredient> findByNameRuIgnoreCase(String nameRu);
}
