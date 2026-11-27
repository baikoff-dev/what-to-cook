package io.github.baikoff.whattocook.pantry.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.baikoff.whattocook.pantry.service.PantryService;

@RestController
@RequestMapping("/api/pantry")
public class PantryRestController {

	private final PantryService pantryService;

	public PantryRestController(PantryService pantryService) {
		this.pantryService = pantryService;
	}

	@GetMapping
	public PantrySettingsResponse getSettings() {
		return PantrySettingsResponse.from(pantryService.getSettings());
	}

	@PutMapping
	public PantrySettingsResponse replaceSettings(@RequestBody PantrySettingsRequest request) {
		pantryService.save(request.ingredientIds());
		return PantrySettingsResponse.from(pantryService.getSettings());
	}
}
