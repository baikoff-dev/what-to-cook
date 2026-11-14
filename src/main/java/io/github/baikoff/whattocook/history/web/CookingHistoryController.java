package io.github.baikoff.whattocook.history.web;

import io.github.baikoff.whattocook.history.service.CookingHistoryService;
import io.github.baikoff.whattocook.history.service.model.CookingRecordForm;
import io.github.baikoff.whattocook.recipe.domain.Recipe;
import io.github.baikoff.whattocook.recipe.service.RecipeCatalogService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/recipes")
public class CookingHistoryController {

	private final RecipeCatalogService recipeCatalogService;
	private final CookingHistoryService cookingHistoryService;

	public CookingHistoryController(
			RecipeCatalogService recipeCatalogService,
			CookingHistoryService cookingHistoryService) {
		this.recipeCatalogService = recipeCatalogService;
		this.cookingHistoryService = cookingHistoryService;
	}

	@GetMapping("/{recipeId}/cookings/new")
	public String createForm(@PathVariable long recipeId, Model model) {
		model.addAttribute("cookingRecordForm", new CookingRecordForm());
		prepareFormModel(recipeId, model);
		return "recipes/cooking-form";
	}

	@PostMapping("/{recipeId}/cookings/new")
	public String create(
			@PathVariable long recipeId,
			@Valid @ModelAttribute("cookingRecordForm") CookingRecordForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			prepareFormModel(recipeId, model);
			return "recipes/cooking-form";
		}

		cookingHistoryService.record(recipeId, form);
		redirectAttributes.addFlashAttribute("message", "Приготовление добавлено в историю");
		return "redirect:/recipes/" + recipeId;
	}

	@GetMapping("/history")
	public String history(Model model) {
		model.addAttribute("history", cookingHistoryService.getHistory());
		return "recipes/history";
	}

	@PostMapping("/history/{recordId}/delete")
	public String delete(
			@PathVariable long recordId,
			RedirectAttributes redirectAttributes) {
		cookingHistoryService.delete(recordId);
		redirectAttributes.addFlashAttribute("message", "Запись удалена из истории");
		return "redirect:/recipes/history";
	}

	private void prepareFormModel(long recipeId, Model model) {
		model.addAttribute("recipe", recipeCatalogService.getRecipe(recipeId));
	}
}
