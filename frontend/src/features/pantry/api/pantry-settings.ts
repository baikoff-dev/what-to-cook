export interface PantryIngredientOption {
  id: number
  name: string
  selected: boolean
}

export interface PantrySettings {
  ingredients: PantryIngredientOption[]
  selectedIngredientNames: string[]
}

export async function loadPantrySettings(): Promise<PantrySettings> {
  const response = await fetch('/api/pantry')

  if (!response.ok) {
    throw new Error('Не удалось загрузить базовую кладовую')
  }

  return response.json() as Promise<PantrySettings>
}

export async function savePantrySettings(
  ingredientIds: number[],
): Promise<PantrySettings> {
  const response = await fetch('/api/pantry', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ingredientIds }),
  })

  if (!response.ok) {
    throw new Error('Не удалось сохранить базовую кладовую')
  }

  return response.json() as Promise<PantrySettings>
}
