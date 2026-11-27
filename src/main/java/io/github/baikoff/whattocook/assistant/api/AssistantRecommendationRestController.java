package io.github.baikoff.whattocook.assistant.api;

import io.github.baikoff.whattocook.assistant.service.DishRecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assistant")
public class AssistantRecommendationRestController {

	private final DishRecommendationService recommendationService;

	public AssistantRecommendationRestController(DishRecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@PostMapping("/recommendations")
	public AssistantRecommendationResponse recommend(@Valid @RequestBody AssistantRecommendationRequest request) {
		if (request.mode() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Режим подбора обязателен");
		}
		return AssistantRecommendationResponse.from(
				recommendationService.recommend(request.toCriteria()));
	}

	@GetMapping("/options")
	public AssistantOptionsResponse getOptions() {
		return AssistantOptionsResponse.from(recommendationService);
	}
}
