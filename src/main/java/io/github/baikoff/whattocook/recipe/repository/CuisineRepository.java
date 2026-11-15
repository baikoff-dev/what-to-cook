package io.github.baikoff.whattocook.recipe.repository;

import io.github.baikoff.whattocook.recipe.domain.Cuisine;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuisineRepository extends JpaRepository<Cuisine, Long> {

	List<Cuisine> findAllByOrderByNameRuAsc();
}
