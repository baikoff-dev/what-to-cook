import { type FormEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  loadPantrySettings,
  savePantrySettings,
  type PantrySettings,
} from '@/features/pantry/api/pantry-settings'

type PantryState =
  | { status: 'loading' }
  | { status: 'success'; settings: PantrySettings }
  | { status: 'error' }

type SaveStatus = 'idle' | 'success' | 'error'

export function PantrySettingsPage() {
  const [state, setState] = useState<PantryState>({ status: 'loading' })
  const [selectedIngredientIds, setSelectedIngredientIds] = useState<number[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [requestNumber, setRequestNumber] = useState(0)
  const [saving, setSaving] = useState(false)
  const [saveStatus, setSaveStatus] = useState<SaveStatus>('idle')

  useEffect(() => {
    let active = true

    loadPantrySettings()
      .then((settings) => {
        if (active) {
          setSelectedIngredientIds(selectedIds(settings))
          setState({ status: 'success', settings })
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' })
        }
      })

    return () => {
      active = false
    }
  }, [requestNumber])

  const toggleIngredient = (ingredientId: number) => {
    setSaveStatus('idle')
    setSelectedIngredientIds((currentIds) =>
      currentIds.includes(ingredientId)
        ? currentIds.filter((id) => id !== ingredientId)
        : [...currentIds, ingredientId],
    )
  }

  const retryLoading = () => {
    setState({ status: 'loading' })
    setSaveStatus('idle')
    setRequestNumber((value) => value + 1)
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setSaveStatus('idle')

    try {
      const settings = await savePantrySettings(selectedIngredientIds)
      setSelectedIngredientIds(selectedIds(settings))
      setState({ status: 'success', settings })
      setSaveStatus('success')
    } catch {
      setSaveStatus('error')
    } finally {
      setSaving(false)
    }
  }

  const visibleIngredients =
    state.status === 'success'
      ? state.settings.ingredients.filter((ingredient) =>
          ingredient.name.toLocaleLowerCase('ru').includes(
            searchQuery.trim().toLocaleLowerCase('ru'),
          ),
        )
      : []

  return (
    <main className="mx-auto w-full max-w-6xl px-5 py-12 sm:px-8 sm:py-16">
      <Link className="text-sm font-medium text-primary hover:underline" to="/">
        ← Вернуться в каталог
      </Link>

      <section className="mt-6 max-w-3xl">
        <p className="text-sm font-semibold tracking-[0.16em] text-primary uppercase">
          Личные настройки
        </p>
        <h1 className="mt-3 font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
          Базовая кладовая
        </h1>
        <p className="mt-4 text-lg text-muted-foreground">
          Отметь продукты, которые почти всегда есть дома. Они автоматически
          учитываются при подборе.
        </p>
      </section>

      {state.status === 'loading' && (
        <p className="mt-8 text-sm text-muted-foreground">Загружаем кладовую...</p>
      )}

      {state.status === 'error' && (
        <div className="mt-8 rounded-2xl border border-destructive/30 bg-background p-6">
          <p className="font-semibold">Не удалось загрузить базовую кладовую</p>
          <p className="mt-2 text-sm text-muted-foreground">
            Проверь, что backend запущен, и попробуй ещё раз.
          </p>
          <Button className="mt-4" onClick={retryLoading} type="button" variant="outline">
            Повторить
          </Button>
        </div>
      )}

      {state.status === 'success' && (
        <>
          <div className="mt-8 rounded-2xl border bg-secondary/40 p-4">
            {state.settings.selectedIngredientNames.length > 0 ? (
              <p>
                Сейчас всегда учитываются:{' '}
                {state.settings.selectedIngredientNames.join(', ')}
              </p>
            ) : (
              <p>Сейчас базовая кладовая пуста.</p>
            )}
          </div>

          <form
            className="mt-6 rounded-3xl border bg-background/90 p-6 shadow-sm sm:p-8"
            onSubmit={submit}
          >
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="font-heading text-2xl font-semibold">Продукты кладовой</h2>
              <Badge variant="secondary">
                Выбрано: {selectedIngredientIds.length}
              </Badge>
            </div>

            <label className="mt-6 grid max-w-md gap-2 text-sm font-medium">
              Найти продукт в списке
              <input
                className="h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20"
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Например, соль или паприка"
                type="search"
                value={searchQuery}
              />
            </label>

            {visibleIngredients.length > 0 ? (
              <div className="mt-6 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {visibleIngredients.map((ingredient) => (
                  <label
                    className="flex cursor-pointer items-center gap-3 rounded-xl border px-4 py-3 hover:bg-muted"
                    key={ingredient.id}
                  >
                    <input
                      checked={selectedIngredientIds.includes(ingredient.id)}
                      onChange={() => toggleIngredient(ingredient.id)}
                      type="checkbox"
                    />
                    <span className="text-sm font-medium">{ingredient.name}</span>
                  </label>
                ))}
              </div>
            ) : (
              <p className="mt-6 rounded-xl border border-dashed p-5 text-sm text-muted-foreground">
                Продукты по запросу не найдены
              </p>
            )}

            <div aria-live="polite" className="mt-5">
              {saveStatus === 'success' && (
                <p className="text-sm font-medium text-primary">
                  Базовая кладовая сохранена
                </p>
              )}
              {saveStatus === 'error' && (
                <p className="text-sm font-medium text-destructive">
                  Не удалось сохранить базовую кладовую
                </p>
              )}
            </div>

            <Button className="mt-4" disabled={saving} type="submit">
              {saving ? 'Сохраняем...' : 'Сохранить кладовую'}
            </Button>
          </form>
        </>
      )}
    </main>
  )
}

function selectedIds(settings: PantrySettings) {
  return settings.ingredients
    .filter((ingredient) => ingredient.selected)
    .map((ingredient) => ingredient.id)
}
