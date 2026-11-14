package io.github.baikoff.whattocook.common.web;

import io.github.baikoff.whattocook.history.web.CookingHistoryController;
import io.github.baikoff.whattocook.recipe.service.RecipeNotFoundException;
import io.github.baikoff.whattocook.recipe.web.RecipeController;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = {RecipeController.class, CookingHistoryController.class})
public class RecipeErrorHandler {

	@ExceptionHandler(RecipeNotFoundException.class)
	public ModelAndView handleNotFound(RecipeNotFoundException exception) {
		var modelAndView = new ModelAndView("error/404");
		modelAndView.setStatus(HttpStatus.NOT_FOUND);
		modelAndView.addObject("message", exception.getMessage());
		return modelAndView;
	}
}
