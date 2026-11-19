import { useEffect, useState } from 'react'
import { AlertTriangle, ArrowLeft, Heart, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  loadFavoriteRecipes,
  setRecipeFavorite,
} from '@/features/favorites/api/recipe-favorites'
import type { RecipeCatalogItem } from '@/features/recipes/api/recipe-catalog'
import { RecipeCard } from '@/features/recipes/components/recipe-card'

type FavoritesState =
  | { status: 'loading' }
  | { status: 'success'; items: RecipeCatalogItem[]; total: number }
  | { status: 'error'; message: string }

export function FavoriteRecipesPage() {
  const [state, setState] = useState<FavoritesState>({ status: 'loading' })
  const [requestNumber, setRequestNumber] = useState(0)
  const [pendingRecipeId, setPendingRecipeId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    loadFavoriteRecipes()
      .then((response) => {
        if (active) {
          setState({ status: 'success', ...response })
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setState({
            status: 'error',
            message:
              error instanceof Error
                ? error.message
                : 'Не удалось загрузить избранные рецепты',
          })
        }
      })

    return () => {
      active = false
    }
  }, [requestNumber])

  const retry = () => {
    setState({ status: 'loading' })
    setRequestNumber((value) => value + 1)
  }

  const removeFavorite = async (recipe: RecipeCatalogItem) => {
    if (pendingRecipeId !== null) {
      return
    }

    setPendingRecipeId(recipe.id)
    setActionError(null)

    try {
      await setRecipeFavorite(recipe.id, false)
      setState((current) => {
        if (current.status !== 'success') {
          return current
        }

        const items = current.items.filter((item) => item.id !== recipe.id)
        return { status: 'success', items, total: items.length }
      })
    } catch (error: unknown) {
      setActionError(
        error instanceof Error ? error.message : 'Не удалось изменить избранное',
      )
    } finally {
      setPendingRecipeId(null)
    }
  }

  return (
    <main className="mx-auto w-full max-w-6xl px-5 py-10 sm:px-8 sm:py-14">
      <Button asChild variant="ghost">
        <Link to="/#recipes">
          <ArrowLeft aria-hidden="true" data-icon="inline-start" />
          Вернуться в каталог
        </Link>
      </Button>

      <header className="mt-7 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div className="max-w-2xl">
          <p className="mb-3 flex items-center gap-2 text-sm font-semibold tracking-[0.16em] text-primary uppercase">
            <Heart aria-hidden="true" className="size-4 fill-current" />
            Личная коллекция
          </p>
          <h1 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
            Избранные рецепты
          </h1>
          <p className="mt-4 text-lg leading-8 text-muted-foreground">
            Блюда, которые хочется сохранить под рукой и приготовить снова.
          </p>
        </div>
        {state.status === 'success' && (
          <Badge variant="secondary">Всего: {state.total}</Badge>
        )}
      </header>

      {actionError && (
        <p className="mt-6 rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive" role="alert">
          {actionError}
        </p>
      )}

      <section className="mt-8" aria-live="polite">
        {state.status === 'loading' && <FavoritesLoading />}
        {state.status === 'error' && (
          <FavoritesError message={state.message} onRetry={retry} />
        )}
        {state.status === 'success' && state.items.length === 0 && (
          <FavoritesEmpty />
        )}
        {state.status === 'success' && state.items.length > 0 && (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {state.items.map((recipe) => (
              <RecipeCard
                favoriteActionPending={pendingRecipeId === recipe.id}
                key={recipe.id}
                onRemoveFavorite={removeFavorite}
                recipe={recipe}
              />
            ))}
          </div>
        )}
      </section>
    </main>
  )
}

function FavoritesLoading() {
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <span className="sr-only">Загружаем избранные рецепты</span>
      {[0, 1, 2].map((item) => (
        <div
          className="h-96 animate-pulse rounded-2xl border bg-card"
          key={item}
        />
      ))}
    </div>
  )
}

function FavoritesEmpty() {
  return (
    <div className="rounded-3xl border bg-card px-6 py-14 text-center shadow-sm">
      <span className="mx-auto grid size-16 place-items-center rounded-full bg-secondary text-primary">
        <Heart aria-hidden="true" className="size-7" />
      </span>
      <h2 className="mt-5 font-heading text-2xl font-semibold">
        В избранном пока нет рецептов
      </h2>
      <p className="mx-auto mt-3 max-w-lg text-muted-foreground">
        Добавь понравившееся блюдо на его странице — оно появится здесь.
      </p>
      <Button asChild className="mt-6">
        <Link to="/#recipes">Перейти в каталог</Link>
      </Button>
    </div>
  )
}

function FavoritesError({
  message,
  onRetry,
}: {
  message: string
  onRetry: () => void
}) {
  return (
    <div className="rounded-3xl border border-destructive/30 bg-card px-6 py-14 text-center">
      <AlertTriangle aria-hidden="true" className="mx-auto size-10 text-destructive" />
      <h2 className="mt-5 font-heading text-2xl font-semibold">
        Избранное пока не загрузилось
      </h2>
      <p className="mt-3 text-muted-foreground" role="alert">{message}</p>
      <Button className="mt-6" onClick={onRetry} variant="outline">
        <RefreshCw aria-hidden="true" data-icon="inline-start" />
        Повторить
      </Button>
    </div>
  )
}
