package io.github.baikoff.whattocook.restriction.service;

import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeIngredient;
import io.github.baikoff.whattocook.recipe.repository.IngredientRepository;
import io.github.baikoff.whattocook.recipe.service.model.SelectionOption;
import io.github.baikoff.whattocook.restriction.domain.IngredientRestriction;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;
import io.github.baikoff.whattocook.restriction.repository.IngredientRestrictionRepository;
import io.github.baikoff.whattocook.restriction.service.model.IngredientRestrictionForm;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionRuleItem;
import io.github.baikoff.whattocook.restriction.service.model.RestrictionWarning;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeRestrictionService {

	private final IngredientRepository ingredientRepository;
	private final IngredientRestrictionRepository restrictionRepository;

	public RecipeRestrictionService(
			IngredientRepository ingredientRepository,
			IngredientRestrictionRepository restrictionRepository) {
		this.ingredientRepository = ingredientRepository;
		this.restrictionRepository = restrictionRepository;
	}

	@Transactional(readOnly = true)
	public List<SelectionOption> getIngredientOptions() {
		return ingredientRepository.findAllByOrderByNameRuAsc()
				.stream()
				.map(ingredient -> new SelectionOption(ingredient.getId(), ingredient.getNameRu()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RestrictionRuleItem> getRules() {
		var restrictions = restrictionRepository.findAllByOrderByIngredientIdAsc();
		var ingredients = ingredientRepository.findAllById(
				restrictions.stream().map(IngredientRestriction::getIngredientId).toList())
				.stream()
				.collect(Collectors.toMap(Ingredient::getId, Function.identity()));

		return restrictions.stream()
				.map(restriction -> toRuleItem(restriction, ingredients.get(restriction.getIngredientId())))
				.sorted(java.util.Comparator.comparing(RestrictionRuleItem::ingredientName, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	@Transactional(readOnly = true)
	public IngredientRestrictionForm getForm(Long ingredientId) {
		if (ingredientId == null) {
			return new IngredientRestrictionForm();
		}
		return restrictionRepository.findById(ingredientId)
				.map(IngredientRestrictionForm::from)
				.orElseGet(IngredientRestrictionForm::new);
	}

	@Transactional
	public boolean save(IngredientRestrictionForm form) {
		ingredientRepository.findById(form.getIngredientId())
				.orElseThrow(() -> new IllegalArgumentException("Выбранный ингредиент не найден"));
		var existing = restrictionRepository.findById(form.getIngredientId());
		var note = normalizeNote(form.getNote());
		if (existing.isPresent()) {
			existing.orElseThrow().update(form.getMode(), note);
			return false;
		}
		restrictionRepository.save(new IngredientRestriction(form.getIngredientId(), form.getMode(), note));
		return true;
	}

	@Transactional
	public void delete(long ingredientId) {
		restrictionRepository.deleteById(ingredientId);
	}

	@Transactional(readOnly = true)
	public RestrictionPolicy loadPolicy() {
		var rules = restrictionRepository.findAll().stream()
				.collect(Collectors.toMap(
						IngredientRestriction::getIngredientId,
						restriction -> new RestrictionPolicy.RestrictionRule(
								restriction.getMode(), restriction.getNote())));
		return new RestrictionPolicy(rules);
	}

	public RecipeRestrictionEvaluation evaluate(Recipe recipe, RestrictionPolicy policy) {
		var warnings = recipe.getIngredients().stream()
				.filter(recipeIngredient -> policy.rules().containsKey(recipeIngredient.getIngredient().getId()))
				.map(recipeIngredient -> toWarning(recipeIngredient, policy.rules().get(
						recipeIngredient.getIngredient().getId())))
				.toList();
		var excluded = warnings.stream().anyMatch(RestrictionWarning::excluding);
		return new RecipeRestrictionEvaluation(excluded, warnings);
	}

	private RestrictionWarning toWarning(
			RecipeIngredient recipeIngredient,
			RestrictionPolicy.RestrictionRule rule) {
		return new RestrictionWarning(
				recipeIngredient.getIngredient().getNameRu(),
				rule.mode().getDisplayName(),
				rule.note(),
				recipeIngredient.isOptional(),
				rule.mode() == RestrictionMode.EXCLUDE);
	}

	private RestrictionRuleItem toRuleItem(IngredientRestriction restriction, Ingredient ingredient) {
		return new RestrictionRuleItem(
				restriction.getIngredientId(),
				ingredient.getNameRu(),
				restriction.getMode(),
				restriction.getMode().getDisplayName(),
				restriction.getNote());
	}

	private String normalizeNote(String note) {
		if (note == null || note.isBlank()) {
			return null;
		}
		return note.strip();
	}
}
