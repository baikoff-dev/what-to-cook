package io.github.baikoff.whattocook.pantry.service;

import io.github.baikoff.whattocook.pantry.domain.PantryIngredient;
import io.github.baikoff.whattocook.pantry.repository.PantryIngredientRepository;
import io.github.baikoff.whattocook.pantry.service.model.PantryOption;
import io.github.baikoff.whattocook.pantry.service.model.PantrySettings;
import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.repository.IngredientRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PantryService {

	private final PantryIngredientRepository pantryIngredientRepository;
	private final IngredientRepository ingredientRepository;

	public PantryService(
			PantryIngredientRepository pantryIngredientRepository,
			IngredientRepository ingredientRepository) {
		this.pantryIngredientRepository = pantryIngredientRepository;
		this.ingredientRepository = ingredientRepository;
	}

	@Transactional(readOnly = true)
	public PantrySettings getSettings() {
		var selectedIds = getIngredientIds();
		var ingredients = ingredientRepository.findAllByOrderByNameRuAsc();
		var options = ingredients.stream()
				.map(ingredient -> new PantryOption(
						ingredient.getId(),
						ingredient.getNameRu(),
						selectedIds.contains(ingredient.getId())))
				.toList();
		var selectedNames = ingredients.stream()
				.filter(ingredient -> selectedIds.contains(ingredient.getId()))
				.map(Ingredient::getNameRu)
				.toList();
		return new PantrySettings(options, selectedNames);
	}

	@Transactional(readOnly = true)
	public Set<Long> getIngredientIds() {
		return pantryIngredientRepository.findAll().stream()
				.map(PantryIngredient::getIngredientId)
				.collect(Collectors.toUnmodifiableSet());
	}

	@Transactional
	public void save(List<Long> requestedIngredientIds) {
		var requestedIds = normalize(requestedIngredientIds);
		var knownIds = ingredientRepository.findAllById(requestedIds).stream()
				.map(Ingredient::getId)
				.collect(Collectors.toSet());
		if (!knownIds.equals(requestedIds)) {
			throw new IllegalArgumentException("Выбран неизвестный ингредиент");
		}

		pantryIngredientRepository.deleteAllInBatch();
		pantryIngredientRepository.saveAll(requestedIds.stream()
				.map(PantryIngredient::new)
				.toList());
	}

	private Set<Long> normalize(List<Long> requestedIngredientIds) {
		if (requestedIngredientIds == null || requestedIngredientIds.isEmpty()) {
			return Set.of();
		}
		return requestedIngredientIds.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
