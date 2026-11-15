package io.github.baikoff.whattocook.assistant.web;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import io.github.baikoff.whattocook.assistant.service.model.AssistantGoal;
import io.github.baikoff.whattocook.recipe.domain.Difficulty;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.validation.Valid;

@Controller
public class AssistantController {

	private final DishRecommendationService recommendationService;

	public AssistantController(DishRecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping("/assistant")
	public String page(
			@Valid @ModelAttribute("assistantForm") AssistantForm form,
			BindingResult bindingResult,
			Model model) {
		model.addAttribute("ingredientOptions", recommendationService.getIngredientOptions());
		model.addAttribute("equipmentOptions", recommendationService.getEquipmentOptions());
		model.addAttribute("cookingMethodOptions", recommendationService.getCookingMethodOptions());
		model.addAttribute("difficultyOptions", Difficulty.values());
		model.addAttribute("goalOptions", AssistantGoal.values());

		var submitted = form.getMode() != null;
		model.addAttribute("submitted", submitted);
		model.addAttribute("validSubmission", submitted && !bindingResult.hasErrors());
		model.addAttribute(
				"recommendations",
				submitted && !bindingResult.hasErrors()
						? recommendationService.recommend(form.toCriteria())
						: List.of());
		return "assistant/index";
	}
}
