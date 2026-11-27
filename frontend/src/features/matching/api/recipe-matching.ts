export interface IngredientOption {
  id: number
  name: string
}

export interface RecipeMatch {
  id: number
  nameRu: string
  description: string
  coverUrl: string | null
  matchPercentage: number
  matchedIngredientCount: number
  requiredIngredientCount: number
  missingIngredients: string[]
  substitutionUsages: Array<{
    originalIngredientName: string
    substituteIngredientName: string
    substitutionType: string
    impactNote: string | null
  }>
  pantryIngredientNames: string[]
  totalMinutes: number
  difficulty: string
  restrictionWarnings: Array<{
    ingredientName: string
    modeName: string
    note: string | null
    optional: boolean
    excluding: boolean
  }>
}

export interface RecipeMatchingPageData {
  ingredients: IngredientOption[]
  selectedIngredientIds: number[]
  recipes: RecipeMatch[]
}

export async function loadRecipeMatches(
  ingredientIds: number[] = [],
): Promise<RecipeMatchingPageData> {
  const searchParams = new URLSearchParams()
  ingredientIds.forEach((ingredientId) => {
    searchParams.append('ingredientIds', String(ingredientId))
  })
  const query = searchParams.toString()
  const response = await fetch(`/api/recipe-matches${query ? `?${query}` : ''}`)

  if (!response.ok) {
    throw new Error('Не удалось подобрать рецепты по продуктам')
  }

  return response.json() as Promise<RecipeMatchingPageData>
}
