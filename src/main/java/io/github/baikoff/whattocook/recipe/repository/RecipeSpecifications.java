package io.github.baikoff.whattocook.recipe.repository;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.Equipment;
import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeSearchCriteria;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;

import java.util.ArrayList;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class RecipeSpecifications {

	private RecipeSpecifications() {
	}

	public static Specification<Recipe> matches(RecipeSearchCriteria criteria) {
		return (root, query, criteriaBuilder) -> {
			var predicates = new ArrayList<Predicate>();
			predicates.add(criteriaBuilder.notEqual(root.get("status"), RecipeStatus.ARCHIVED));

			if (criteria.query() != null) {
				var ingredient = root.join("ingredients", JoinType.LEFT)
						.join("ingredient", JoinType.LEFT);
				var pattern = containsPattern(criteria.query());
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("nameRu")), pattern, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("nameEn")), pattern, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(ingredient.get("nameRu")), pattern, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(ingredient.get("nameEn")), pattern, '\\')));
			}

			if (criteria.maxMinutes() != null) {
				var totalMinutes = criteriaBuilder.sum(
						root.<Integer>get("preparationMinutes"),
						root.<Integer>get("cookingMinutes"));
				predicates.add(criteriaBuilder.le(totalMinutes, criteria.maxMinutes()));
			}

			if (criteria.difficulty() != null) {
				predicates.add(criteriaBuilder.equal(root.get("difficulty"), criteria.difficulty()));
			}

			if (criteria.equipmentId() != null) {
				predicates.add(criteriaBuilder.equal(
						root.join("equipment", JoinType.INNER).get("id"),
						criteria.equipmentId()));
			}

			if (criteria.cookingMethodId() != null) {
				predicates.add(criteriaBuilder.equal(
						root.join("cookingMethods", JoinType.INNER).get("id"),
						criteria.cookingMethodId()));
			}

			query.distinct(true);
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String containsPattern(String value) {
		var escaped = value.toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escaped + "%";
	}
}
