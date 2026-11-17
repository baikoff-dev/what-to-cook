package io.github.baikoff.whattocook.pantry.repository;

import io.github.baikoff.whattocook.pantry.domain.PantryIngredient;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PantryIngredientRepository extends JpaRepository<PantryIngredient, Long> {
}
