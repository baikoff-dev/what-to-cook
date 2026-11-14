package io.github.baikoff.whattocook.restriction.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredient_restrictions")
public class IngredientRestriction {

	@Id
	@Column(name = "ingredient_id")
	private Long ingredientId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RestrictionMode mode;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected IngredientRestriction() {
	}

	public IngredientRestriction(Long ingredientId, RestrictionMode mode, String note) {
		this.ingredientId = ingredientId;
		this.mode = mode;
		this.note = note;
	}

	public void update(RestrictionMode mode, String note) {
		this.mode = mode;
		this.note = note;
	}

	@PrePersist
	void beforeInsert() {
		var now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = Instant.now();
	}

	public Long getIngredientId() {
		return ingredientId;
	}

	public RestrictionMode getMode() {
		return mode;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
