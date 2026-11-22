import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  Archive,
  ArrowLeft,
  ChefHat,
  Clock3,
  RefreshCw,
  RotateCcw,
  UsersRound,
} from 'lucide-react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import {
  loadArchivedRecipes,
  setRecipeArchived,
} from '@/features/archive/api/recipe-archive'
import type { RecipeCatalogItem } from '@/features/recipes/api/recipe-catalog'
import { backendUrl } from '@/lib/backend-url'

type ArchiveState =
  | { status: 'loading' }
  | { status: 'success'; items: RecipeCatalogItem[]; total: number }
  | { status: 'error'; message: string }

export function RecipeArchivePage() {
  const [state, setState] = useState<ArchiveState>({ status: 'loading' })
  const [requestNumber, setRequestNumber] = useState(0)
  const [pendingRecipeId, setPendingRecipeId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    loadArchivedRecipes()
      .then((archive) => {
        if (active) setState({ status: 'success', ...archive })
      })
      .catch((error: unknown) => {
        if (active) {
          setState({
            status: 'error',
            message:
              error instanceof Error
                ? error.message
                : 'Не удалось загрузить архив рецептов',
          })
        }
      })

    return () => {
      active = false
    }
  }, [requestNumber])

  const restore = async (recipe: RecipeCatalogItem) => {
    if (pendingRecipeId !== null) return
    setPendingRecipeId(recipe.id)
    setActionError(null)

    try {
      await setRecipeArchived(recipe.id, false)
      setState((current) => {
        if (current.status !== 'success') return current
        const items = current.items.filter((item) => item.id !== recipe.id)
        return { status: 'success', items, total: items.length }
      })
    } catch (error: unknown) {
      setActionError(
        error instanceof Error ? error.message : 'Не удалось восстановить рецепт',
      )
    } finally {
      setPendingRecipeId(null)
    }
  }

  const retry = () => {
    setState({ status: 'loading' })
    setRequestNumber((value) => value + 1)
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
            <Archive aria-hidden="true" className="size-4" />
            Отложенные рецепты
          </p>
          <h1 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
            Архив рецептов
          </h1>
          <p className="mt-4 text-lg leading-8 text-muted-foreground">
            Архив скрывает рецепт из каталога, но сохраняет его историю,
            избранное и рабочий статус.
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
        {state.status === 'loading' && <ArchiveLoading />}
        {state.status === 'error' && (
          <ArchiveError message={state.message} onRetry={retry} />
        )}
        {state.status === 'success' && state.items.length === 0 && <ArchiveEmpty />}
        {state.status === 'success' && state.items.length > 0 && (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {state.items.map((recipe) => (
              <ArchivedRecipeCard
                key={recipe.id}
                onRestore={restore}
                pending={pendingRecipeId === recipe.id}
                recipe={recipe}
              />
            ))}
          </div>
        )}
      </section>
    </main>
  )
}

function ArchivedRecipeCard({
  onRestore,
  pending,
  recipe,
}: {
  onRestore: (recipe: RecipeCatalogItem) => void
  pending: boolean
  recipe: RecipeCatalogItem
}) {
  return (
    <Card className="overflow-hidden bg-background/90 py-0 shadow-sm">
      <div className="aspect-[4/3] overflow-hidden bg-secondary">
        {recipe.coverUrl ? (
          <img
            alt={`Обложка блюда «${recipe.name}»`}
            className="size-full object-cover grayscale-[25%]"
            src={backendUrl(recipe.coverUrl)}
          />
        ) : (
          <div className="grid size-full place-items-center text-primary">
            <ChefHat aria-hidden="true" className="size-12" />
          </div>
        )}
      </div>
      <CardHeader className="gap-3 pt-5">
        <div className="flex flex-wrap gap-2">
          <Badge variant="secondary">{recipe.cuisine}</Badge>
          <Badge variant="outline">{recipe.status}</Badge>
        </div>
        <div>
          <h2 className="font-heading text-xl leading-tight font-semibold">
            {recipe.name}
          </h2>
          {recipe.englishName && (
            <p className="mt-1 text-sm text-muted-foreground">{recipe.englishName}</p>
          )}
        </div>
      </CardHeader>
      <CardContent className="pb-5">
        <p className="line-clamp-3 min-h-18 text-sm leading-6 text-muted-foreground">
          {recipe.description}
        </p>
        <div className="mt-5 flex flex-wrap gap-x-4 gap-y-2 text-sm font-medium">
          <span className="flex items-center gap-1.5">
            <Clock3 aria-hidden="true" className="size-4 text-primary" />
            {recipe.totalMinutes} мин
          </span>
          <span className="flex items-center gap-1.5">
            <UsersRound aria-hidden="true" className="size-4 text-primary" />
            {recipe.servings} порц.
          </span>
          <span>{recipe.difficulty}</span>
        </div>
        <Button
          aria-label={`Восстановить рецепт: ${recipe.name}`}
          className="mt-5 w-full"
          disabled={pending}
          onClick={() => onRestore(recipe)}
          type="button"
          variant="outline"
        >
          <RotateCcw aria-hidden="true" data-icon="inline-start" />
          {pending ? 'Восстанавливаем...' : 'Восстановить'}
        </Button>
      </CardContent>
    </Card>
  )
}

function ArchiveLoading() {
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <span className="sr-only">Загружаем архив рецептов</span>
      {[0, 1, 2].map((item) => (
        <div className="h-96 animate-pulse rounded-2xl border bg-card" key={item} />
      ))}
    </div>
  )
}

function ArchiveEmpty() {
  return (
    <div className="rounded-3xl border bg-card px-6 py-14 text-center shadow-sm">
      <span className="mx-auto grid size-16 place-items-center rounded-full bg-secondary text-primary">
        <Archive aria-hidden="true" className="size-7" />
      </span>
      <h2 className="mt-5 font-heading text-2xl font-semibold">
        В архиве пока нет рецептов
      </h2>
      <p className="mx-auto mt-3 max-w-lg text-muted-foreground">
        Архивированные блюда появятся здесь, и их всегда можно будет восстановить.
      </p>
      <Button asChild className="mt-6">
        <Link to="/#recipes">Перейти в каталог</Link>
      </Button>
    </div>
  )
}

function ArchiveError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="rounded-3xl border border-destructive/30 bg-card px-6 py-14 text-center">
      <AlertTriangle aria-hidden="true" className="mx-auto size-10 text-destructive" />
      <h2 className="mt-5 font-heading text-2xl font-semibold">Архив пока не загрузился</h2>
      <p className="mt-3 text-muted-foreground" role="alert">{message}</p>
      <Button className="mt-6" onClick={onRetry} variant="outline">
        <RefreshCw aria-hidden="true" data-icon="inline-start" />
        Повторить
      </Button>
    </div>
  )
}
