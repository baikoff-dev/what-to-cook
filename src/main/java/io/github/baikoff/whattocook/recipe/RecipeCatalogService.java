package io.github.baikoff.whattocook.recipe;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeCatalogService {

	private final RecipeRepository recipeRepository;
	private final RecipeImageStorage imageStorage;
	private final RecipePortionCalculator portionCalculator;

	public RecipeCatalogService(
			RecipeRepository recipeRepository,
			RecipeImageStorage imageStorage,
			RecipePortionCalculator portionCalculator) {
		this.recipeRepository = recipeRepository;
		this.imageStorage = imageStorage;
		this.portionCalculator = portionCalculator;
	}

	@Transactional(readOnly = true)
	public List<RecipeCatalogItem> getArchive() {
		return recipeRepository.findAllByStatusOrderByNameRuAsc(RecipeStatus.ARCHIVED)
				.stream()
				.map(this::toCatalogItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RecipeCatalogItem> getCatalog() {
		return search(RecipeSearchCriteria.empty());
	}

	@Transactional(readOnly = true)
	public List<RecipeCatalogItem> search(RecipeSearchCriteria criteria) {
		return recipeRepository.findAll(
				RecipeSpecifications.matches(criteria),
				Sort.by(Sort.Direction.ASC, "nameRu"))
				.stream()
				.map(this::toCatalogItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public RecipeDetails getRecipe(long id) {
		return getRecipe(id, null);
	}

	@Transactional(readOnly = true)
	public RecipeDetails getRecipe(long id, Integer requestedServings) {
		var recipe = recipeRepository.findByIdAndStatusNot(id, RecipeStatus.ARCHIVED)
				.orElseThrow(() -> new RecipeNotFoundException(id));

		return toDetails(recipe, requestedServings);
	}

	private RecipeCatalogItem toCatalogItem(Recipe recipe) {
		return new RecipeCatalogItem(
				recipe.getId(),
				recipe.getNameRu(),
				recipe.getNameEn(),
				recipe.getDescription(),
				imageStorage.publicUrl(recipe.getMainImagePath()),
				totalMinutes(recipe),
				recipe.getServings(),
				recipe.getDifficulty().getDisplayName(),
				recipe.getStatus().getDisplayName());
	}

	private RecipeDetails toDetails(Recipe recipe, Integer requestedServings) {
		var servings = portionCalculator.resolveServings(requestedServings, recipe.getServings());
		var ingredients = recipe.getIngredients()
				.stream()
				.map(recipeIngredient -> new RecipeDetails.IngredientLine(
						recipeIngredient.getIngredient().getNameRu(),
						formatAmount(portionCalculator.recalculate(
								recipeIngredient.getAmount(),
								recipe.getServings(),
								servings)),
						recipeIngredient.getUnit().getDisplayName(),
						recipeIngredient.isOptional()))
				.toList();

		var steps = recipe.getSteps()
				.stream()
				.map(step -> new RecipeDetails.Step(
						step.getSortOrder(),
						step.getInstruction(),
						imageStorage.publicUrl(step.getImagePath())))
				.toList();

		var equipment = recipe.getEquipment()
				.stream()
				.map(Equipment::getNameRu)
				.toList();

		var cookingMethods = recipe.getCookingMethods()
				.stream()
				.map(CookingMethod::getNameRu)
				.toList();

		return new RecipeDetails(
				recipe.getId(),
				recipe.getNameRu(),
				recipe.getNameEn(),
				recipe.getDescription(),
				imageStorage.publicUrl(recipe.getMainImagePath()),
				recipe.getServings(),
				servings,
				recipe.getPreparationMinutes(),
				recipe.getCookingMinutes(),
				totalMinutes(recipe),
				recipe.getDifficulty().getDisplayName(),
				recipe.getStatus().getDisplayName(),
				recipe.getSourceName(),
				recipe.getSourceUrl(),
				recipe.getPersonalNotes(),
				ingredients,
				steps,
				equipment,
				cookingMethods);
	}

	private int totalMinutes(Recipe recipe) {
		return recipe.getPreparationMinutes() + recipe.getCookingMinutes();
	}

	private String formatAmount(BigDecimal amount) {
		return amount.stripTrailingZeros().toPlainString();
	}
}
