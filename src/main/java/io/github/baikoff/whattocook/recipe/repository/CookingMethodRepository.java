package io.github.baikoff.whattocook.recipe.repository;

import io.github.baikoff.whattocook.recipe.domain.CookingMethod;
import io.github.baikoff.whattocook.recipe.domain.Recipe;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CookingMethodRepository extends JpaRepository<CookingMethod, Long> {

	List<CookingMethod> findAllByOrderByNameRuAsc();
}
