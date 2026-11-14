package io.github.baikoff.whattocook.restriction.repository;

import io.github.baikoff.whattocook.restriction.domain.IngredientRestriction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRestrictionRepository extends JpaRepository<IngredientRestriction, Long> {

	List<IngredientRestriction> findAllByOrderByIngredientIdAsc();
}
