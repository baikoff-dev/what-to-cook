package io.github.baikoff.whattocook.history.repository;

import io.github.baikoff.whattocook.history.domain.CookingRecord;
import io.github.baikoff.whattocook.recipe.domain.Recipe;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookingRecordRepository extends JpaRepository<CookingRecord, Long> {

	@EntityGraph(attributePaths = "recipe")
	List<CookingRecord> findAllByOrderByCookedOnDescCreatedAtDesc();
}
