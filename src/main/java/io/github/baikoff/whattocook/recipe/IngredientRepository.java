package io.github.baikoff.whattocook.recipe;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface IngredientRepository extends JpaRepository<Ingredient, Long> {

	Optional<Ingredient> findByNameRuIgnoreCase(String nameRu);
}
