export interface CookingHistoryItem {
  id: number
  recipeId: number
  recipeName: string
  cookedOn: string
  rating: number | null
  note: string | null
  archived: boolean
}

export interface CookingHistoryResponse {
  items: CookingHistoryItem[]
  total: number
}

export interface CookingRecordInput {
  cookedOn: string
  rating: number | null
  note: string
}

export class CookingHistoryApiError extends Error {
  fieldErrors: Record<string, string>

  constructor(message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.fieldErrors = fieldErrors
  }
}

export async function loadCookingHistory(): Promise<CookingHistoryResponse> {
  const response = await fetch('/api/cooking-history')

  if (!response.ok) {
    throw new Error('Не удалось загрузить историю приготовления')
  }

  return response.json() as Promise<CookingHistoryResponse>
}

export async function addCookingRecord(
  recipeId: number,
  input: CookingRecordInput,
): Promise<{ id: number }> {
  const response = await fetch(`/api/recipes/${recipeId}/cookings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as
      | { message?: string; fieldErrors?: Record<string, string> }
      | null
    throw new CookingHistoryApiError(
      error?.message ?? 'Не удалось добавить приготовление',
      error?.fieldErrors ?? {},
    )
  }

  return response.json() as Promise<{ id: number }>
}

export async function deleteCookingRecord(recordId: number): Promise<void> {
  const response = await fetch(`/api/cooking-history/${recordId}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    throw new Error('Не удалось удалить запись из истории')
  }
}
