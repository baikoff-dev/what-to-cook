package io.github.baikoff.whattocook.recipe.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipe_id", nullable = false)
	private Recipe recipe;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ingredient_id", nullable = false)
	private Ingredient ingredient;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MeasurementUnit unit;

	@Column(nullable = false)
	private boolean optional;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@OneToMany(mappedBy = "recipeIngredient", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<IngredientSubstitution> substitutions = new ArrayList<>();

	protected RecipeIngredient() {
	}

	RecipeIngredient(Recipe recipe, Ingredient ingredient, BigDecimal amount, MeasurementUnit unit,
			boolean optional, int sortOrder) {
		this.recipe = recipe;
		this.ingredient = ingredient;
		this.amount = amount;
		this.unit = unit;
		this.optional = optional;
		this.sortOrder = sortOrder;
	}

	public void addSubstitution(
			Ingredient substituteIngredient,
			BigDecimal amount,
			MeasurementUnit unit,
			SubstitutionType type,
			String impactNote,
			int sortOrder) {
		substitutions.add(new IngredientSubstitution(
				this,
				substituteIngredient,
				amount,
				unit,
				type,
				impactNote,
				sortOrder));
	}

	public Long getId() {
		return id;
	}

	public Recipe getRecipe() {
		return recipe;
	}

	public Ingredient getIngredient() {
		return ingredient;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public MeasurementUnit getUnit() {
		return unit;
	}

	public boolean isOptional() {
		return optional;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public List<IngredientSubstitution> getSubstitutions() {
		return substitutions;
	}
}
