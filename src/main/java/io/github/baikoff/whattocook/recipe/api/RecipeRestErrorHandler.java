package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.recipe.service.RecipeNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = {
		"io.github.baikoff.whattocook.recipe.api",
		"io.github.baikoff.whattocook.favorite.api"
})
public class RecipeRestErrorHandler {

	@ExceptionHandler(RecipeNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(RecipeNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	public record ApiErrorResponse(String message) {
	}
}
