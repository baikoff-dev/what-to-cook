package io.github.baikoff.whattocook.recipe;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = RecipeController.class)
public class RecipeErrorHandler {

	@ExceptionHandler(RecipeNotFoundException.class)
	public ModelAndView handleNotFound(RecipeNotFoundException exception) {
		var modelAndView = new ModelAndView("error/404");
		modelAndView.setStatus(HttpStatus.NOT_FOUND);
		modelAndView.addObject("message", exception.getMessage());
		return modelAndView;
	}
}
