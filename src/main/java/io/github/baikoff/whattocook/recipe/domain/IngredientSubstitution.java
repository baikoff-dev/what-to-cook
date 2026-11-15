package io.github.baikoff.whattocook.recipe.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredient_substitutions")
public class IngredientSubstitution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipe_ingredient_id", nullable = false)
	private RecipeIngredient recipeIngredient;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "substitute_ingredient_id", nullable = false)
	private Ingredient substituteIngredient;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MeasurementUnit unit;

	@Enumerated(EnumType.STRING)
	@Column(name = "substitution_type", nullable = false, length = 30)
	private SubstitutionType type;

	@Column(name = "impact_note", nullable = false, length = 1000)
	private String impactNote;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected IngredientSubstitution() {
	}

	IngredientSubstitution(
			RecipeIngredient recipeIngredient,
			Ingredient substituteIngredient,
			BigDecimal amount,
			MeasurementUnit unit,
			SubstitutionType type,
			String impactNote,
			int sortOrder) {
		this.recipeIngredient = recipeIngredient;
		this.substituteIngredient = substituteIngredient;
		this.amount = amount;
		this.unit = unit;
		this.type = type;
		this.impactNote = impactNote;
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return id;
	}

	public Ingredient getSubstituteIngredient() {
		return substituteIngredient;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public MeasurementUnit getUnit() {
		return unit;
	}

	public SubstitutionType getType() {
		return type;
	}

	public String getImpactNote() {
		return impactNote;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
