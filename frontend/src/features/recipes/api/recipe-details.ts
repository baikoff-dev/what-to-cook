export interface RecipeDetails {
  id: number
  name: string
  englishName: string | null
  description: string
  coverUrl: string | null
  baseServings: number
  servings: number
  preparationMinutes: number
  cookingMinutes: number
  totalMinutes: number
  difficulty: string
  status: string
  cuisine: string
  origin: string
  sourceName: string | null
  sourceUrl: string | null
  videoUrl: string | null
  personalNotes: string | null
  ingredients: RecipeIngredient[]
  steps: RecipeStep[]
  equipment: string[]
  cookingMethods: string[]
  favorite: boolean
  excludedByRestriction: boolean
  restrictionWarnings: RestrictionWarning[]
}

export interface RecipeIngredient {
  name: string
  amount: string
  unit: string
  optional: boolean
  substitutions: IngredientSubstitution[]
}

export interface IngredientSubstitution {
  ingredientName: string
  amount: string
  unit: string
  type: string
  impactNote: string
}

export interface RecipeStep {
  number: number
  instruction: string
  imageUrl: string | null
}

export interface RestrictionWarning {
  ingredientName: string
  modeName: string
  note: string | null
  optional: boolean
  excluding: boolean
}

export async function loadRecipeDetails(
  recipeId: number,
  servings?: number,
): Promise<RecipeDetails> {
  const query = servings === undefined ? '' : `?servings=${servings}`
  const response = await fetch(`/api/recipes/${recipeId}${query}`)

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as
      | { message?: string }
      | null
    throw new Error(error?.message ?? 'Не удалось загрузить рецепт')
  }

  return response.json() as Promise<RecipeDetails>
}
