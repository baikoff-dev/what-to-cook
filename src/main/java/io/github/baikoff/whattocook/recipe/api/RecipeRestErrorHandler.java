package io.github.baikoff.whattocook.recipe.api;

import io.github.baikoff.whattocook.recipe.service.RecipeConflictException;
import io.github.baikoff.whattocook.recipe.service.RecipeNotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = {
		"io.github.baikoff.whattocook.recipe.api",
		"io.github.baikoff.whattocook.favorite.api",
		"io.github.baikoff.whattocook.history.api",
		"io.github.baikoff.whattocook.restriction.api"
})
public class RecipeRestErrorHandler {

	@ExceptionHandler(RecipeNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(RecipeNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(RecipeConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(RecipeConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleValidation(
			MethodArgumentNotValidException exception) {
		var fieldErrors = new LinkedHashMap<String, String>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(new ValidationErrorResponse(fieldErrors));
	}

	public record ApiErrorResponse(String message) {
	}

	public record ValidationErrorResponse(Map<String, String> fieldErrors) {
	}
}
