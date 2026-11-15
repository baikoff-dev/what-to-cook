package io.github.baikoff.whattocook.recipe.domain;

public record RecipeSearchCriteria(
		String query,
		Integer maxMinutes,
		Difficulty difficulty,
		Long equipmentId,
		Long cookingMethodId,
		Long cuisineId,
		RecipeOrigin origin) {

	public RecipeSearchCriteria {
		query = normalizeQuery(query);
	}

	public static RecipeSearchCriteria empty() {
		return new RecipeSearchCriteria(null, null, null, null, null, null, null);
	}

	public boolean hasFilters() {
		return query != null
				|| maxMinutes != null
				|| difficulty != null
				|| equipmentId != null
				|| cookingMethodId != null
				|| cuisineId != null
				|| origin != null;
	}

	private static String normalizeQuery(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
