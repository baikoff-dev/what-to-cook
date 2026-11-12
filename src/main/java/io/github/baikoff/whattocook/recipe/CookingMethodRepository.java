package io.github.baikoff.whattocook.recipe;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface CookingMethodRepository extends JpaRepository<CookingMethod, Long> {

	List<CookingMethod> findAllByOrderByNameRuAsc();
}
