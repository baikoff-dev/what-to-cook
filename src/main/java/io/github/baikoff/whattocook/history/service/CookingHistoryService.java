package io.github.baikoff.whattocook.history.service;

import io.github.baikoff.whattocook.history.domain.CookingRecord;
import io.github.baikoff.whattocook.history.repository.CookingRecordRepository;
import io.github.baikoff.whattocook.history.service.model.CookingHistoryItem;
import io.github.baikoff.whattocook.history.service.model.CookingRecordForm;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.repository.RecipeRepository;
import io.github.baikoff.whattocook.recipe.service.RecipeNotFoundException;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CookingHistoryService {

	private final RecipeRepository recipeRepository;
	private final CookingRecordRepository cookingRecordRepository;

	public CookingHistoryService(
			RecipeRepository recipeRepository,
			CookingRecordRepository cookingRecordRepository) {
		this.recipeRepository = recipeRepository;
		this.cookingRecordRepository = cookingRecordRepository;
	}

	@Transactional
	public long record(long recipeId, CookingRecordForm form) {
		var recipe = recipeRepository.findByIdAndStatusNot(recipeId, RecipeStatus.ARCHIVED)
				.orElseThrow(() -> new RecipeNotFoundException(recipeId));
		var record = new CookingRecord(
				recipe,
				form.getCookedOn(),
				form.getRating(),
				normalizeNote(form.getNote()));
		return cookingRecordRepository.save(record).getId();
	}

	@Transactional(readOnly = true)
	public List<CookingHistoryItem> getHistory() {
		return cookingRecordRepository.findAllByOrderByCookedOnDescCreatedAtDesc()
				.stream()
				.map(this::toHistoryItem)
				.toList();
	}

	@Transactional
	public void delete(long recordId) {
		cookingRecordRepository.deleteById(recordId);
	}

	private CookingHistoryItem toHistoryItem(CookingRecord record) {
		var recipe = record.getRecipe();
		return new CookingHistoryItem(
				record.getId(),
				recipe.getId(),
				recipe.getNameRu(),
				record.getCookedOn(),
				record.getRating(),
				record.getNote(),
				recipe.getStatus() == RecipeStatus.ARCHIVED);
	}

	private String normalizeNote(String note) {
		if (note == null || note.isBlank()) {
			return null;
		}
		return note.strip();
	}
}
