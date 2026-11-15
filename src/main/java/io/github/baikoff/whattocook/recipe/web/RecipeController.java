package io.github.baikoff.whattocook.recipe.web;

import io.github.baikoff.whattocook.favorite.service.RecipeFavoriteService;
import io.github.baikoff.whattocook.image.storage.ImageStorageException;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;
import io.github.baikoff.whattocook.recipe.domain.MeasurementUnit;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.domain.RecipeSearchCriteria;
import io.github.baikoff.whattocook.recipe.domain.RecipeOrigin;
import io.github.baikoff.whattocook.recipe.domain.RecipeStatus;
import io.github.baikoff.whattocook.recipe.domain.SubstitutionType;
import io.github.baikoff.whattocook.recipe.service.model.RecipeForm;
import io.github.baikoff.whattocook.recipe.service.RecipeCatalogService;
import io.github.baikoff.whattocook.recipe.service.RecipeConflictException;
import io.github.baikoff.whattocook.recipe.service.RecipeManagementService;
import io.github.baikoff.whattocook.recipe.service.RecipeMatchingService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/recipes")
public class RecipeController {

	private final RecipeCatalogService recipeCatalogService;
	private final RecipeManagementService recipeManagementService;
	private final RecipeMatchingService recipeMatchingService;
	private final RecipeFavoriteService recipeFavoriteService;
	private final RecipeFormValidator recipeFormValidator;

	public RecipeController(
			RecipeCatalogService recipeCatalogService,
			RecipeManagementService recipeManagementService,
			RecipeMatchingService recipeMatchingService,
			RecipeFavoriteService recipeFavoriteService,
			RecipeFormValidator recipeFormValidator) {
		this.recipeCatalogService = recipeCatalogService;
		this.recipeManagementService = recipeManagementService;
		this.recipeMatchingService = recipeMatchingService;
		this.recipeFavoriteService = recipeFavoriteService;
		this.recipeFormValidator = recipeFormValidator;
	}

	@InitBinder("recipeForm")
	void configureRecipeFormValidation(WebDataBinder binder) {
		binder.addValidators(recipeFormValidator);
	}

	@GetMapping
	public String catalog(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) Integer maxMinutes,
			@RequestParam(required = false) Difficulty difficulty,
			@RequestParam(required = false) Long equipmentId,
			@RequestParam(required = false) Long cookingMethodId,
			@RequestParam(required = false) Long cuisineId,
			@RequestParam(required = false) RecipeOrigin origin,
			Model model) {
		var search = new RecipeSearchCriteria(
				query,
				maxMinutes,
				difficulty,
				equipmentId,
				cookingMethodId,
				cuisineId,
				origin);
		model.addAttribute("recipes", recipeCatalogService.search(search));
		model.addAttribute("search", search);
		model.addAttribute("filtersApplied", search.hasFilters());
		model.addAttribute("equipmentOptions", recipeManagementService.getEquipmentOptions());
		model.addAttribute("cookingMethodOptions", recipeManagementService.getCookingMethodOptions());
		model.addAttribute("cuisineOptions", recipeManagementService.getCuisineOptions());
		model.addAttribute("originOptions", RecipeOrigin.values());
		model.addAttribute("difficultyOptions", Difficulty.values());
		model.addAttribute("pageTitle", "Каталог рецептов");
		model.addAttribute("archivedView", false);
		model.addAttribute("favoritesView", false);
		model.addAttribute("catalogView", true);
		return "recipes/catalog";
	}

	@GetMapping("/favorites")
	public String favorites(Model model) {
		model.addAttribute("recipes", recipeCatalogService.getFavorites());
		model.addAttribute("pageTitle", "Избранные рецепты");
		model.addAttribute("archivedView", false);
		model.addAttribute("favoritesView", true);
		model.addAttribute("catalogView", false);
		model.addAttribute("filtersApplied", false);
		return "recipes/catalog";
	}

	@GetMapping("/archive")
	public String archive(Model model) {
		model.addAttribute("recipes", recipeCatalogService.getArchive());
		model.addAttribute("pageTitle", "Архив рецептов");
		model.addAttribute("archivedView", true);
		model.addAttribute("favoritesView", false);
		model.addAttribute("catalogView", false);
		model.addAttribute("filtersApplied", false);
		return "recipes/catalog";
	}

	@GetMapping("/match")
	public String match(
			@RequestParam(required = false) List<Long> ingredientIds,
			Model model) {
		model.addAttribute("matching", recipeMatchingService.match(ingredientIds));
		return "recipes/match";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("recipeForm", RecipeForm.empty());
		prepareFormModel(model, "Новый рецепт", "/recipes/new");
		return "recipes/form";
	}

	@PostMapping("/new")
	public String create(
			@Valid @ModelAttribute("recipeForm") RecipeForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			prepareFormModel(model, "Новый рецепт", "/recipes/new");
			return "recipes/form";
		}

		try {
			var recipeId = recipeManagementService.create(form);
			redirectAttributes.addFlashAttribute("message", "Рецепт создан");
			return "redirect:/recipes/" + recipeId;
		} catch (IllegalArgumentException | ImageStorageException exception) {
			bindingResult.reject("recipe.save.failed", exception.getMessage());
			prepareFormModel(model, "Новый рецепт", "/recipes/new");
			return "recipes/form";
		}
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable long id, Model model) {
		model.addAttribute("recipeForm", recipeManagementService.getForm(id));
		prepareFormModel(model, "Редактирование рецепта", "/recipes/" + id + "/edit");
		return "recipes/form";
	}

	@PostMapping("/{id}/edit")
	public String update(
			@PathVariable long id,
			@Valid @ModelAttribute("recipeForm") RecipeForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			prepareFormModel(model, "Редактирование рецепта", "/recipes/" + id + "/edit");
			return "recipes/form";
		}

		try {
			var recipeId = recipeManagementService.update(id, form);
			redirectAttributes.addFlashAttribute("message", "Изменения сохранены");
			return "redirect:/recipes/" + recipeId;
		} catch (RecipeConflictException | IllegalArgumentException | ImageStorageException exception) {
			bindingResult.reject("recipe.save.failed", exception.getMessage());
			prepareFormModel(model, "Редактирование рецепта", "/recipes/" + id + "/edit");
			return "recipes/form";
		}
	}

	@PostMapping("/{id}/archive")
	public String archive(@PathVariable long id, RedirectAttributes redirectAttributes) {
		recipeManagementService.archive(id);
		redirectAttributes.addFlashAttribute("message", "Рецепт перемещён в архив");
		return "redirect:/recipes";
	}

	@PostMapping("/{id}/restore")
	public String restore(@PathVariable long id, RedirectAttributes redirectAttributes) {
		recipeManagementService.restore(id);
		redirectAttributes.addFlashAttribute("message", "Рецепт восстановлен");
		return "redirect:/recipes/" + id;
	}

	@PostMapping("/{id}/favorite")
	public String addFavorite(
			@PathVariable long id,
			@RequestParam(required = false) String returnTo,
			RedirectAttributes redirectAttributes) {
		recipeFavoriteService.add(id);
		redirectAttributes.addFlashAttribute("message", "Рецепт добавлен в избранное");
		return "redirect:" + resolveReturnPath(id, returnTo);
	}

	@PostMapping("/{id}/favorite/remove")
	public String removeFavorite(
			@PathVariable long id,
			@RequestParam(required = false) String returnTo,
			RedirectAttributes redirectAttributes) {
		recipeFavoriteService.remove(id);
		redirectAttributes.addFlashAttribute("message", "Рецепт удалён из избранного");
		return "redirect:" + resolveReturnPath(id, returnTo);
	}

	@GetMapping("/{id}")
	public String details(
			@PathVariable long id,
			@RequestParam(required = false) Integer servings,
			Model model) {
		try {
			model.addAttribute("recipe", recipeCatalogService.getRecipe(id, servings));
		} catch (IllegalArgumentException exception) {
			model.addAttribute("recipe", recipeCatalogService.getRecipe(id));
			model.addAttribute("portionError", exception.getMessage());
		}
		return "recipes/details";
	}

	private void prepareFormModel(Model model, String formTitle, String formAction) {
		model.addAttribute("formTitle", formTitle);
		model.addAttribute("formAction", formAction);
		model.addAttribute("equipmentOptions", recipeManagementService.getEquipmentOptions());
		model.addAttribute("cookingMethodOptions", recipeManagementService.getCookingMethodOptions());
		model.addAttribute("cuisineOptions", recipeManagementService.getCuisineOptions());
		model.addAttribute("difficultyOptions", Difficulty.values());
		model.addAttribute("originOptions", RecipeOrigin.values());
		model.addAttribute("statusOptions", List.of(
				RecipeStatus.UNTESTED,
				RecipeStatus.TESTED,
				RecipeStatus.NEEDS_CHANGES));
		model.addAttribute("unitOptions", MeasurementUnit.values());
		model.addAttribute("substitutionTypeOptions", SubstitutionType.values());
	}

	private String resolveReturnPath(long recipeId, String returnTo) {
		if ("/recipes".equals(returnTo) || "/recipes/favorites".equals(returnTo)) {
			return returnTo;
		}
		return "/recipes/" + recipeId;
	}
}
