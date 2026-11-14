package io.github.baikoff.whattocook.history.domain;

import io.github.baikoff.whattocook.recipe.domain.Recipe;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "cooking_records")
public class CookingRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipe_id", nullable = false)
	private Recipe recipe;

	@Column(name = "cooked_on", nullable = false)
	private LocalDate cookedOn;

	@Column
	private Integer rating;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected CookingRecord() {
	}

	public CookingRecord(Recipe recipe, LocalDate cookedOn, Integer rating, String note) {
		this.recipe = recipe;
		this.cookedOn = cookedOn;
		this.rating = rating;
		this.note = note;
	}

	@PrePersist
	void beforeInsert() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Recipe getRecipe() {
		return recipe;
	}

	public LocalDate getCookedOn() {
		return cookedOn;
	}

	public Integer getRating() {
		return rating;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
