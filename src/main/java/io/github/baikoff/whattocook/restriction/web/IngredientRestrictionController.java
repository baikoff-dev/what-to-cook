package io.github.baikoff.whattocook.restriction.web;

import io.github.baikoff.whattocook.recipe.domain.Ingredient;
import io.github.baikoff.whattocook.restriction.domain.RestrictionMode;
import io.github.baikoff.whattocook.restriction.service.model.IngredientRestrictionForm;
import io.github.baikoff.whattocook.restriction.service.RecipeRestrictionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/preferences/restrictions")
public class IngredientRestrictionController {

	private final RecipeRestrictionService restrictionService;

	public IngredientRestrictionController(RecipeRestrictionService restrictionService) {
		this.restrictionService = restrictionService;
	}

	@GetMapping
	public String page(
			@RequestParam(required = false) Long ingredientId,
			Model model) {
		model.addAttribute("ingredientRestrictionForm", restrictionService.getForm(ingredientId));
		prepareModel(model);
		return "preferences/restrictions";
	}

	@PostMapping
	public String save(
			@Valid @ModelAttribute("ingredientRestrictionForm") IngredientRestrictionForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			prepareModel(model);
			return "preferences/restrictions";
		}

		try {
			var created = restrictionService.save(form);
			redirectAttributes.addFlashAttribute(
					"message",
					created ? "Ограничение добавлено" : "Ограничение обновлено");
			return "redirect:/preferences/restrictions";
		} catch (IllegalArgumentException exception) {
			bindingResult.rejectValue("ingredientId", "restriction.ingredient.invalid", exception.getMessage());
			prepareModel(model);
			return "preferences/restrictions";
		}
	}

	@PostMapping("/{ingredientId}/delete")
	public String delete(
			@PathVariable long ingredientId,
			RedirectAttributes redirectAttributes) {
		restrictionService.delete(ingredientId);
		redirectAttributes.addFlashAttribute("message", "Ограничение удалено");
		return "redirect:/preferences/restrictions";
	}

	private void prepareModel(Model model) {
		model.addAttribute("ingredientOptions", restrictionService.getIngredientOptions());
		model.addAttribute("modeOptions", RestrictionMode.values());
		model.addAttribute("rules", restrictionService.getRules());
	}
}
