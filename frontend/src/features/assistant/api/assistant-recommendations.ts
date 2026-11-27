export interface AssistantRecommendation {
  id: number
  nameRu: string
  description: string
  coverUrl: string | null
  totalMinutes: number
  difficulty: string
  equipment: string[]
  cookingMethods: string[]
  ingredientsSpecified: boolean
  matchPercentage: number
  matchedIngredientCount: number
  requiredIngredientCount: number
  missingIngredients: string[]
  substitutionUsages: IngredientSubstitutionUsage[]
  pantryIngredientNames: string[]
  lastCookedOn: string | null
  favorite: boolean
  averageRating: number | null
  reasons: string[]
  restrictionWarnings: RestrictionWarning[]
}

interface IngredientSubstitutionUsage {
  originalIngredientName: string
  substituteIngredientName: string
  substitutionType: string
  impactNote: string
}

interface RestrictionWarning {
  ingredientName: string
  modeName: string
  note: string | null
  optional: boolean
  excluding: boolean
}

export interface AssistantRecommendationResponse {
  items: AssistantRecommendation[]
}

interface IdOption {
  id: number
  name: string
}

interface EnumOption {
  value: string
  name: string
}

export interface AssistantOptions {
  ingredients: IdOption[]
  equipment: IdOption[]
  cookingMethods: IdOption[]
  difficulties: EnumOption[]
  goals: EnumOption[]
}

export interface AssistantPreferences {
  ingredientIds: number[]
  maxMinutes?: number
  equipmentId?: number
  cookingMethodId?: number
  maxDifficulty?: string
  goal: string
}

export async function loadAssistantOptions(): Promise<AssistantOptions> {
  const response = await fetch('/api/assistant/options')

  if (!response.ok) {
    throw new Error('Не удалось загрузить параметры помощника')
  }

  return response.json() as Promise<AssistantOptions>
}

export async function loadSurpriseRecommendations(): Promise<AssistantRecommendationResponse> {
  const response = await fetch('/api/assistant/recommendations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mode: 'SURPRISE' }),
  })

  if (!response.ok) {
    throw new Error('Не удалось подобрать блюда')
  }

  return response.json() as Promise<AssistantRecommendationResponse>
}

export async function loadPreferenceRecommendations(
  preferences: AssistantPreferences,
): Promise<AssistantRecommendationResponse> {
  const response = await fetch('/api/assistant/recommendations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mode: 'PREFERENCES', ...preferences }),
  })

  if (!response.ok) {
    throw new Error('Не удалось подобрать блюда')
  }

  return response.json() as Promise<AssistantRecommendationResponse>
}
