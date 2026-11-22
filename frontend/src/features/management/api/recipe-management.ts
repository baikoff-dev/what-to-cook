export interface EnumOption {
  value: string
  name: string
}

export interface IdOption {
  id: number
  name: string
}

export interface RecipeFormOptions {
  difficulties: EnumOption[]
  statuses: EnumOption[]
  origins: EnumOption[]
  measurementUnits: EnumOption[]
  substitutionTypes: EnumOption[]
  equipment: IdOption[]
  cookingMethods: IdOption[]
  cuisines: IdOption[]
}

export interface RecipeIngredientInput {
  name: string
  amount: number
  unit: string
  optional: boolean
}

export interface RecipeSubstitutionInput {
  originalIngredientName: string
  substituteIngredientName: string
  amount: number
  unit: string
  type: string
  impactNote: string
}

export interface RecipeStepInput {
  id: number | null
  instruction: string
}

export interface RecipeInput {
  version: number
  nameRu: string
  nameEn: string
  description: string
  servings: number
  preparationMinutes: number
  cookingMinutes: number
  difficulty: string
  status: string
  cuisineId: number
  origin: string
  sourceName: string
  sourceUrl: string
  videoUrl: string
  personalNotes: string
  equipmentIds: number[]
  cookingMethodIds: number[]
  ingredients: RecipeIngredientInput[]
  substitutions: RecipeSubstitutionInput[]
  steps: RecipeStepInput[]
}

export type RecipeEditResponse = Omit<
  RecipeInput,
  'nameEn' | 'sourceName' | 'sourceUrl' | 'videoUrl' | 'personalNotes' | 'steps'
> & {
  id: number
  nameEn: string | null
  sourceName: string | null
  sourceUrl: string | null
  videoUrl: string | null
  personalNotes: string | null
  steps: Array<RecipeStepInput & { imageUrl: string | null }>
  mainImageUrl: string | null
}

export class RecipeManagementApiError extends Error {
  fieldErrors: Record<string, string>

  constructor(message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.fieldErrors = fieldErrors
  }
}

export async function loadRecipeFormOptions(): Promise<RecipeFormOptions> {
  const response = await fetch('/api/recipes/form-options')

  if (!response.ok) {
    throw new Error('Не удалось загрузить справочники формы рецепта')
  }

  return response.json() as Promise<RecipeFormOptions>
}

export async function loadRecipeForEditing(recipeId: number): Promise<RecipeEditResponse> {
  const response = await fetch(`/api/recipes/${recipeId}/edit`)

  if (!response.ok) {
    throw new Error('Не удалось загрузить рецепт для редактирования')
  }

  return response.json() as Promise<RecipeEditResponse>
}

export async function createRecipe(input: RecipeInput): Promise<{ id: number }> {
  const response = await fetch('/api/recipes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  await ensureSaved(response)

  return response.json() as Promise<{ id: number }>
}

export async function updateRecipe(
  recipeId: number,
  input: RecipeInput,
): Promise<{ id: number }> {
  const response = await fetch(`/api/recipes/${recipeId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  await ensureSaved(response)

  return response.json() as Promise<{ id: number }>
}

async function ensureSaved(response: Response) {
  if (response.ok) {
    return
  }

  const error = (await response.json().catch(() => null)) as
    | { message?: string; fieldErrors?: Record<string, string> }
    | null
  throw new RecipeManagementApiError(
    error?.message ?? 'Не удалось сохранить рецепт',
    error?.fieldErrors ?? {},
  )
}
