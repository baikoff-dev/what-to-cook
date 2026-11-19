export interface RestrictionWarning {
  ingredientName: string
  modeName: string
  note: string | null
  optional: boolean
  excluding: boolean
}

export interface RecipeCatalogItem {
  id: number
  name: string
  englishName: string
  description: string
  coverUrl: string | null
  totalMinutes: number
  servings: number
  difficulty: string
  status: string
  cuisine: string
  origin: string
  favorite: boolean
  restrictionWarnings: RestrictionWarning[]
}

export interface RecipeCatalogResponse {
  items: RecipeCatalogItem[]
  total: number
}

export interface RecipeCatalogFilters {
  query: string
  maxMinutes: string
  difficulty: string
  equipmentId: string
  cookingMethodId: string
  cuisineId: string
  origin: string
}

interface IdOption {
  id: number
  name: string
}

interface EnumOption {
  value: string
  name: string
}

export interface RecipeCatalogFilterOptions {
  difficulties: EnumOption[]
  equipment: IdOption[]
  cookingMethods: IdOption[]
  cuisines: IdOption[]
  origins: EnumOption[]
}

export const emptyRecipeCatalogFilters: RecipeCatalogFilters = {
  query: '',
  maxMinutes: '',
  difficulty: '',
  equipmentId: '',
  cookingMethodId: '',
  cuisineId: '',
  origin: '',
}

export async function loadRecipeCatalog(
  filters: RecipeCatalogFilters = emptyRecipeCatalogFilters,
): Promise<RecipeCatalogResponse> {
  const parameters = new URLSearchParams()
  Object.entries(filters).forEach(([name, value]) => {
    if (value) {
      parameters.set(name, value)
    }
  })
  const query = parameters.toString()
  const response = await fetch(query ? `/api/recipes?${query}` : '/api/recipes')

  if (!response.ok) {
    throw new Error('Не удалось загрузить каталог рецептов')
  }

  return response.json() as Promise<RecipeCatalogResponse>
}

export async function loadRecipeCatalogFilterOptions(): Promise<RecipeCatalogFilterOptions> {
  const response = await fetch('/api/recipes/filter-options')

  if (!response.ok) {
    throw new Error('Не удалось загрузить варианты фильтров')
  }

  return response.json() as Promise<RecipeCatalogFilterOptions>
}
