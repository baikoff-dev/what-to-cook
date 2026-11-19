import type { RecipeCatalogResponse } from '@/features/recipes/api/recipe-catalog'

export interface RecipeFavoriteResponse {
  favorite: boolean
}

export async function loadFavoriteRecipes(): Promise<RecipeCatalogResponse> {
  const response = await fetch('/api/recipes/favorites')

  if (!response.ok) {
    throw new Error('Не удалось загрузить избранные рецепты')
  }

  return response.json() as Promise<RecipeCatalogResponse>
}

export async function setRecipeFavorite(
  recipeId: number,
  favorite: boolean,
): Promise<RecipeFavoriteResponse> {
  const response = await fetch(`/api/recipes/${recipeId}/favorite`, {
    method: favorite ? 'PUT' : 'DELETE',
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as
      | { message?: string }
      | null
    throw new Error(error?.message ?? 'Не удалось изменить избранное')
  }

  return response.json() as Promise<RecipeFavoriteResponse>
}
