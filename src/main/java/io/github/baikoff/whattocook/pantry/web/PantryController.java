package io.github.baikoff.whattocook.pantry.web;

import io.github.baikoff.whattocook.pantry.service.PantryService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/preferences/pantry")
public class PantryController {

	private final PantryService pantryService;

	public PantryController(PantryService pantryService) {
		this.pantryService = pantryService;
	}

	@GetMapping
	public String page(Model model) {
		model.addAttribute("pantry", pantryService.getSettings());
		return "preferences/pantry";
	}

	@PostMapping
	public String save(
			@RequestParam(required = false) List<Long> ingredientIds,
			RedirectAttributes redirectAttributes) {
		pantryService.save(ingredientIds);
		redirectAttributes.addFlashAttribute("message", "Базовая кладовая сохранена");
		return "redirect:/preferences/pantry";
	}
}
