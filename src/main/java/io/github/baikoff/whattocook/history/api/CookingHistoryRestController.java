package io.github.baikoff.whattocook.history.api;

import io.github.baikoff.whattocook.history.service.CookingHistoryService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CookingHistoryRestController {

	private final CookingHistoryService cookingHistoryService;

	public CookingHistoryRestController(CookingHistoryService cookingHistoryService) {
		this.cookingHistoryService = cookingHistoryService;
	}

	@PostMapping("/api/recipes/{recipeId}/cookings")
	public ResponseEntity<CookingRecordCreatedResponse> record(
			@PathVariable long recipeId,
			@Valid @RequestBody CookingRecordRequest request) {
		var recordId = cookingHistoryService.record(recipeId, request.toForm());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new CookingRecordCreatedResponse(recordId));
	}

	@GetMapping("/api/cooking-history")
	public CookingHistoryResponse getHistory() {
		return CookingHistoryResponse.from(cookingHistoryService.getHistory());
	}

	@DeleteMapping("/api/cooking-history/{recordId}")
	public ResponseEntity<Void> delete(@PathVariable long recordId) {
		cookingHistoryService.delete(recordId);
		return ResponseEntity.noContent().build();
	}

	public record CookingRecordCreatedResponse(long id) {
	}
}
