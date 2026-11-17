package io.github.baikoff.whattocook.pantry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pantry_ingredients")
public class PantryIngredient {

	@Id
	@Column(name = "ingredient_id")
	private Long ingredientId;

	protected PantryIngredient() {
	}

	public PantryIngredient(Long ingredientId) {
		this.ingredientId = ingredientId;
	}

	public Long getIngredientId() {
		return ingredientId;
	}
}
