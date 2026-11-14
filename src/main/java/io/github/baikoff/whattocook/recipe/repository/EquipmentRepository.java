package io.github.baikoff.whattocook.recipe.repository;

import io.github.baikoff.whattocook.recipe.domain.Equipment;
import io.github.baikoff.whattocook.recipe.domain.Recipe;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

	List<Equipment> findAllByOrderByNameRuAsc();
}
