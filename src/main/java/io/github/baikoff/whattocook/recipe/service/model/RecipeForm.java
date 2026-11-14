package io.github.baikoff.whattocook.recipe.service.model;

import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.MeasurementUnit;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RecipeForm {

	private Long id;
	private long version;

	@NotBlank(message = "Укажи русское название")
	@Size(max = 200, message = "Название должно быть не длиннее 200 символов")
	private String nameRu;

	@Size(max = 200, message = "Английское название должно быть не длиннее 200 символов")
	private String nameEn;

	@NotBlank(message = "Добавь описание")
	@Size(max = 5000, message = "Описание должно быть не длиннее 5000 символов")
	private String description;

	@Min(value = 1, message = "Должна быть хотя бы одна порция")
	@Max(value = 100, message = "Количество порций должно быть не больше 100")
	private int servings = 1;

	@Min(value = 0, message = "Время не может быть отрицательным")
	@Max(value = 1440, message = "Время подготовки должно быть не больше суток")
	private int preparationMinutes;

	@Min(value = 0, message = "Время не может быть отрицательным")
	@Max(value = 1440, message = "Время приготовления должно быть не больше суток")
	private int cookingMinutes;

	@NotNull(message = "Выбери сложность")
	private Difficulty difficulty = Difficulty.EASY;

	@NotNull(message = "Выбери статус")
	private RecipeStatus status = RecipeStatus.UNTESTED;

	@Size(max = 300, message = "Название источника должно быть не длиннее 300 символов")
	private String sourceName;

	@Size(max = 1000, message = "Ссылка должна быть не длиннее 1000 символов")
	@Pattern(regexp = "^$|https?://.+", message = "Ссылка должна начинаться с http:// или https://")
	private String sourceUrl;

	@Size(max = 5000, message = "Заметка должна быть не длиннее 5000 символов")
	private String personalNotes;

	@NotEmpty(message = "Выбери хотя бы один вид оборудования")
	private List<Long> equipmentIds = new ArrayList<>();

	@NotEmpty(message = "Выбери хотя бы один способ приготовления")
	private List<Long> cookingMethodIds = new ArrayList<>();

	@Valid
	@NotEmpty(message = "Добавь хотя бы один ингредиент")
	private List<IngredientRow> ingredients = new ArrayList<>();

	@Valid
	@NotEmpty(message = "Добавь хотя бы один шаг")
	private List<StepRow> steps = new ArrayList<>();

	private MultipartFile mainImage;
	private String existingMainImagePath;
	private boolean removeMainImage;

	public static RecipeForm empty() {
		var form = new RecipeForm();
		form.ingredients.add(new IngredientRow());
		form.steps.add(new StepRow());
		return form;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getNameRu() {
		return nameRu;
	}

	public void setNameRu(String nameRu) {
		this.nameRu = nameRu;
	}

	public String getNameEn() {
		return nameEn;
	}

	public void setNameEn(String nameEn) {
		this.nameEn = nameEn;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getServings() {
		return servings;
	}

	public void setServings(int servings) {
		this.servings = servings;
	}

	public int getPreparationMinutes() {
		return preparationMinutes;
	}

	public void setPreparationMinutes(int preparationMinutes) {
		this.preparationMinutes = preparationMinutes;
	}

	public int getCookingMinutes() {
		return cookingMinutes;
	}

	public void setCookingMinutes(int cookingMinutes) {
		this.cookingMinutes = cookingMinutes;
	}

	public Difficulty getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}

	public RecipeStatus getStatus() {
		return status;
	}

	public void setStatus(RecipeStatus status) {
		this.status = status;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getPersonalNotes() {
		return personalNotes;
	}

	public void setPersonalNotes(String personalNotes) {
		this.personalNotes = personalNotes;
	}

	public List<Long> getEquipmentIds() {
		return equipmentIds;
	}

	public void setEquipmentIds(List<Long> equipmentIds) {
		this.equipmentIds = equipmentIds;
	}

	public List<Long> getCookingMethodIds() {
		return cookingMethodIds;
	}

	public void setCookingMethodIds(List<Long> cookingMethodIds) {
		this.cookingMethodIds = cookingMethodIds;
	}

	public List<IngredientRow> getIngredients() {
		return ingredients;
	}

	public void setIngredients(List<IngredientRow> ingredients) {
		this.ingredients = ingredients;
	}

	public List<StepRow> getSteps() {
		return steps;
	}

	public void setSteps(List<StepRow> steps) {
		this.steps = steps;
	}

	public MultipartFile getMainImage() {
		return mainImage;
	}

	public void setMainImage(MultipartFile mainImage) {
		this.mainImage = mainImage;
	}

	public String getExistingMainImagePath() {
		return existingMainImagePath;
	}

	public void setExistingMainImagePath(String existingMainImagePath) {
		this.existingMainImagePath = existingMainImagePath;
	}

	public boolean isRemoveMainImage() {
		return removeMainImage;
	}

	public void setRemoveMainImage(boolean removeMainImage) {
		this.removeMainImage = removeMainImage;
	}

	public static class IngredientRow {

		@NotBlank(message = "Укажи ингредиент")
		@Size(max = 150, message = "Название должно быть не длиннее 150 символов")
		private String name;

		@NotNull(message = "Укажи количество")
		@DecimalMin(value = "0.001", message = "Количество должно быть больше нуля")
		@Digits(integer = 7, fraction = 3, message = "Допустимо не больше трёх знаков после запятой")
		private BigDecimal amount;

		@NotNull(message = "Выбери единицу измерения")
		private MeasurementUnit unit;

		private boolean optional;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public MeasurementUnit getUnit() {
			return unit;
		}

		public void setUnit(MeasurementUnit unit) {
			this.unit = unit;
		}

		public boolean isOptional() {
			return optional;
		}

		public void setOptional(boolean optional) {
			this.optional = optional;
		}
	}

	public static class StepRow {

		private Long id;

		@NotBlank(message = "Опиши шаг")
		@Size(max = 5000, message = "Шаг должен быть не длиннее 5000 символов")
		private String instruction;

		private MultipartFile image;
		private String existingImagePath;
		private boolean removeImage;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getInstruction() {
			return instruction;
		}

		public void setInstruction(String instruction) {
			this.instruction = instruction;
		}

		public MultipartFile getImage() {
			return image;
		}

		public void setImage(MultipartFile image) {
			this.image = image;
		}

		public String getExistingImagePath() {
			return existingImagePath;
		}

		public void setExistingImagePath(String existingImagePath) {
			this.existingImagePath = existingImagePath;
		}

		public boolean isRemoveImage() {
			return removeImage;
		}

		public void setRemoveImage(boolean removeImage) {
			this.removeImage = removeImage;
		}
	}
}
