import type { RecipeCatalogResponse } from '@/features/recipes/api/recipe-catalog'

export interface RecipeArchiveResponse {
  archived: boolean
}

export async function loadArchivedRecipes(): Promise<RecipeCatalogResponse> {
  const response = await fetch('/api/recipes/archive')

  if (!response.ok) {
    throw new Error('Не удалось загрузить архив рецептов')
  }

  return response.json() as Promise<RecipeCatalogResponse>
}

export async function setRecipeArchived(
  recipeId: number,
  archived: boolean,
): Promise<RecipeArchiveResponse> {
  const response = await fetch(`/api/recipes/${recipeId}/archive`, {
    method: archived ? 'PUT' : 'DELETE',
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as
      | { message?: string }
      | null
    throw new Error(
      error?.message ??
        (archived
          ? 'Не удалось перенести рецепт в архив'
          : 'Не удалось восстановить рецепт'),
    )
  }

  return response.json() as Promise<RecipeArchiveResponse>
}
