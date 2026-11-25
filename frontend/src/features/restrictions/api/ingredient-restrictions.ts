export interface IngredientRestrictionOption {
  id: number
  name: string
}

export interface RestrictionModeOption {
  value: 'WARN' | 'EXCLUDE'
  name: string
}

export interface IngredientRestrictionRule {
  ingredientId: number
  ingredientName: string
  mode: RestrictionModeOption['value']
  modeName: string
  note: string | null
}

export interface IngredientRestrictionPage {
  ingredients: IngredientRestrictionOption[]
  modes: RestrictionModeOption[]
  rules: IngredientRestrictionRule[]
  total: number
}

export interface IngredientRestrictionInput {
  mode: RestrictionModeOption['value']
  note: string
}

export class IngredientRestrictionRequestError extends Error {
  fieldErrors: Record<string, string>

  constructor(message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.fieldErrors = fieldErrors
  }
}

export async function loadIngredientRestrictions(): Promise<IngredientRestrictionPage> {
  const response = await fetch('/api/ingredient-restrictions')

  if (!response.ok) {
    throw new Error('Не удалось загрузить ограничения по ингредиентам')
  }

  return response.json() as Promise<IngredientRestrictionPage>
}

export async function saveIngredientRestriction(
  ingredientId: number,
  input: IngredientRestrictionInput,
): Promise<void> {
  const response = await fetch(`/api/ingredient-restrictions/${ingredientId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as
      | { message?: string; fieldErrors?: Record<string, string> }
      | null
    throw new IngredientRestrictionRequestError(
      error?.message ?? 'Не удалось сохранить ограничение',
      error?.fieldErrors ?? {},
    )
  }
}

export async function deleteIngredientRestriction(ingredientId: number): Promise<void> {
  const response = await fetch(`/api/ingredient-restrictions/${ingredientId}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw new Error('Не удалось удалить ограничение')
  }
}
