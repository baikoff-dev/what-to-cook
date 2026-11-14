package io.github.baikoff.whattocook.favorite.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_favorites")
public class RecipeFavorite {

	@Id
	@Column(name = "recipe_id")
	private Long recipeId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected RecipeFavorite() {
	}

	public RecipeFavorite(Long recipeId) {
		this.recipeId = recipeId;
	}

	@PrePersist
	void beforeInsert() {
		createdAt = Instant.now();
	}

	public Long getRecipeId() {
		return recipeId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
