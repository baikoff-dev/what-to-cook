package io.github.baikoff.whattocook.recipe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_steps")
public class RecipeStep {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipe_id", nullable = false)
	private Recipe recipe;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false, columnDefinition = "text")
	private String instruction;

	@Column(name = "image_path", length = 500)
	private String imagePath;

	protected RecipeStep() {
	}

	RecipeStep(Recipe recipe, int sortOrder, String instruction, String imagePath) {
		this.recipe = recipe;
		this.sortOrder = sortOrder;
		this.instruction = instruction;
		this.imagePath = imagePath;
	}

	public Long getId() {
		return id;
	}

	public Recipe getRecipe() {
		return recipe;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public String getInstruction() {
		return instruction;
	}

	public String getImagePath() {
		return imagePath;
	}
}
