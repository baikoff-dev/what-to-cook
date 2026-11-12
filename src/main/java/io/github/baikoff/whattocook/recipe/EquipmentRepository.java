package io.github.baikoff.whattocook.recipe;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface EquipmentRepository extends JpaRepository<Equipment, Long> {

	List<Equipment> findAllByOrderByNameRuAsc();
}
