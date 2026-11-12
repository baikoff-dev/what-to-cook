package io.github.baikoff.whattocook.recipe;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/recipes")
public class RecipeController {

	private final RecipeCatalogService recipeCatalogService;
	private final RecipeManagementService recipeManagementService;
	private final RecipeFormValidator recipeFormValidator;

	public RecipeController(
			RecipeCatalogService recipeCatalogService,
			RecipeManagementService recipeManagementService,
			RecipeFormValidator recipeFormValidator) {
		this.recipeCatalogService = recipeCatalogService;
		this.recipeManagementService = recipeManagementService;
		this.recipeFormValidator = recipeFormValidator;
	}

	@InitBinder("recipeForm")
	void configureRecipeFormValidation(WebDataBinder binder) {
		binder.addValidators(recipeFormValidator);
	}

	@GetMapping
	public String catalog(Model model) {
		model.addAttribute("recipes", recipeCatalogService.getCatalog());
		model.addAttribute("pageTitle", "Каталог рецептов");
		model.addAttribute("archivedView", false);
		return "recipes/catalog";
	}

	@GetMapping("/archive")
	public String archive(Model model) {
		model.addAttribute("recipes", recipeCatalogService.getArchive());
		model.addAttribute("pageTitle", "Архив рецептов");
		model.addAttribute("archivedView", true);
		return "recipes/catalog";
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

	@GetMapping("/{id}")
	public String details(@PathVariable long id, Model model) {
		model.addAttribute("recipe", recipeCatalogService.getRecipe(id));
		return "recipes/details";
	}

	private void prepareFormModel(Model model, String formTitle, String formAction) {
		model.addAttribute("formTitle", formTitle);
		model.addAttribute("formAction", formAction);
		model.addAttribute("equipmentOptions", recipeManagementService.getEquipmentOptions());
		model.addAttribute("cookingMethodOptions", recipeManagementService.getCookingMethodOptions());
		model.addAttribute("difficultyOptions", Difficulty.values());
		model.addAttribute("statusOptions", List.of(
				RecipeStatus.UNTESTED,
				RecipeStatus.TESTED,
				RecipeStatus.NEEDS_CHANGES));
		model.addAttribute("unitOptions", MeasurementUnit.values());
	}
}
