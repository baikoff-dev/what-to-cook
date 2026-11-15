package io.github.baikoff.whattocook.recipe.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "recipes")
public class Recipe {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@Column(name = "name_ru", nullable = false, length = 200)
	private String nameRu;

	@Column(name = "name_en", length = 200)
	private String nameEn;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Column(nullable = false)
	private int servings;

	@Column(name = "preparation_minutes", nullable = false)
	private int preparationMinutes;

	@Column(name = "cooking_minutes", nullable = false)
	private int cookingMinutes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Difficulty difficulty;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RecipeStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cuisine_id", nullable = false)
	private Cuisine cuisine;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RecipeOrigin origin;

	@Column(name = "source_name", length = 300)
	private String sourceName;

	@Column(name = "source_url", length = 1000)
	private String sourceUrl;

	@Column(name = "main_image_path", length = 500)
	private String mainImagePath;

	@Column(name = "personal_notes", columnDefinition = "text")
	private String personalNotes;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<RecipeIngredient> ingredients = new ArrayList<>();

	@OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<RecipeStep> steps = new ArrayList<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "recipe_equipment",
			joinColumns = @JoinColumn(name = "recipe_id"),
			inverseJoinColumns = @JoinColumn(name = "equipment_id"))
	@OrderBy("nameRu ASC")
	private List<Equipment> equipment = new ArrayList<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "recipe_cooking_methods",
			joinColumns = @JoinColumn(name = "recipe_id"),
			inverseJoinColumns = @JoinColumn(name = "cooking_method_id"))
	@OrderBy("nameRu ASC")
	private List<CookingMethod> cookingMethods = new ArrayList<>();

	protected Recipe() {
	}

	public Recipe(String slug) {
		this.slug = slug;
	}

	public void updateDetails(
			String nameRu,
			String nameEn,
			String description,
			int servings,
			int preparationMinutes,
			int cookingMinutes,
			Difficulty difficulty,
			RecipeStatus status,
			Cuisine cuisine,
			RecipeOrigin origin,
			String sourceName,
			String sourceUrl,
			String personalNotes) {
		this.nameRu = nameRu;
		this.nameEn = nameEn;
		this.description = description;
		this.servings = servings;
		this.preparationMinutes = preparationMinutes;
		this.cookingMinutes = cookingMinutes;
		this.difficulty = difficulty;
		this.status = status;
		this.cuisine = cuisine;
		this.origin = origin;
		this.sourceName = sourceName;
		this.sourceUrl = sourceUrl;
		this.personalNotes = personalNotes;
	}

	public void replaceEquipment(List<Equipment> equipment) {
		this.equipment.clear();
		this.equipment.addAll(equipment);
	}

	public void replaceCookingMethods(List<CookingMethod> cookingMethods) {
		this.cookingMethods.clear();
		this.cookingMethods.addAll(cookingMethods);
	}

	public void clearIngredients() {
		ingredients.clear();
	}

	public RecipeIngredient addIngredient(Ingredient ingredient, java.math.BigDecimal amount, MeasurementUnit unit,
			boolean optional, int sortOrder) {
		var recipeIngredient = new RecipeIngredient(this, ingredient, amount, unit, optional, sortOrder);
		ingredients.add(recipeIngredient);
		return recipeIngredient;
	}

	public void clearSteps() {
		steps.clear();
	}

	public void addStep(int sortOrder, String instruction, String imagePath) {
		steps.add(new RecipeStep(this, sortOrder, instruction, imagePath));
	}

	public void setMainImagePath(String mainImagePath) {
		this.mainImagePath = mainImagePath;
	}

	public void archive() {
		status = RecipeStatus.ARCHIVED;
	}

	public void restore() {
		status = RecipeStatus.UNTESTED;
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

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public String getNameRu() {
		return nameRu;
	}

	public String getNameEn() {
		return nameEn;
	}

	public String getDescription() {
		return description;
	}

	public int getServings() {
		return servings;
	}

	public int getPreparationMinutes() {
		return preparationMinutes;
	}

	public int getCookingMinutes() {
		return cookingMinutes;
	}

	public Difficulty getDifficulty() {
		return difficulty;
	}

	public RecipeStatus getStatus() {
		return status;
	}

	public Cuisine getCuisine() {
		return cuisine;
	}

	public RecipeOrigin getOrigin() {
		return origin;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public String getMainImagePath() {
		return mainImagePath;
	}

	public String getPersonalNotes() {
		return personalNotes;
	}

	public long getVersion() {
		return version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<RecipeIngredient> getIngredients() {
		return ingredients;
	}

	public List<RecipeStep> getSteps() {
		return steps;
	}

	public List<Equipment> getEquipment() {
		return equipment;
	}

	public List<CookingMethod> getCookingMethods() {
		return cookingMethods;
	}
}
