import { type FormEvent, useEffect, useState } from 'react'
import {
  AlertTriangle,
  ArrowLeft,
  ChefHat,
  Clock3,
  ExternalLink,
  Heart,
  NotebookPen,
  Pencil,
  Play,
  RefreshCw,
  RotateCcw,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { setRecipeFavorite } from '@/features/favorites/api/recipe-favorites'
import {
  loadRecipeDetails,
  type RecipeDetails,
} from '@/features/recipes/api/recipe-details'
import { backendUrl } from '@/lib/backend-url'

type DetailsState =
  | { status: 'loading' }
  | { status: 'success'; recipe: RecipeDetails }
  | { status: 'error'; message: string }

export function RecipeDetailsPage() {
  const { recipeId } = useParams()
  const id = Number(recipeId)
  const [state, setState] = useState<DetailsState>({ status: 'loading' })
  const [servings, setServings] = useState('')
  const [favoritePending, setFavoritePending] = useState(false)
  const [favoriteError, setFavoriteError] = useState<string | null>(null)

  useEffect(() => {
    if (!Number.isInteger(id) || id < 1) {
      return
    }

    let active = true
    loadRecipeDetails(id)
      .then((recipe) => {
        if (active) {
          setState({ status: 'success', recipe })
          setServings(String(recipe.servings))
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setState({
            status: 'error',
            message:
              error instanceof Error ? error.message : 'Не удалось загрузить рецепт',
          })
        }
      })

    return () => {
      active = false
    }
  }, [id])

  const load = (requestedServings?: number) => {
    if (!Number.isInteger(id) || id < 1) {
      setState({ status: 'error', message: 'Некорректный номер рецепта' })
      return
    }

    setState({ status: 'loading' })
    loadRecipeDetails(id, requestedServings)
      .then((recipe) => {
        setState({ status: 'success', recipe })
        setServings(String(recipe.servings))
      })
      .catch((error: unknown) => {
        setState({
          status: 'error',
          message: error instanceof Error ? error.message : 'Не удалось загрузить рецепт',
        })
      })
  }

  const recalculate = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const requestedServings = Number(servings)

    if (!Number.isInteger(requestedServings) || requestedServings < 1 || requestedServings > 100) {
      setState({
        status: 'error',
        message: 'Количество порций должно быть от 1 до 100',
      })
      return
    }

    load(requestedServings)
  }

  const toggleFavorite = async () => {
    if (state.status !== 'success' || favoritePending) {
      return
    }

    const nextFavorite = !state.recipe.favorite
    setFavoritePending(true)
    setFavoriteError(null)

    try {
      const result = await setRecipeFavorite(id, nextFavorite)
      setState((current) =>
        current.status === 'success'
          ? {
              status: 'success',
              recipe: { ...current.recipe, favorite: result.favorite },
            }
          : current,
      )
    } catch (error: unknown) {
      setFavoriteError(
        error instanceof Error ? error.message : 'Не удалось изменить избранное',
      )
    } finally {
      setFavoritePending(false)
    }
  }

  if (!Number.isInteger(id) || id < 1) {
    return (
      <DetailsError
        message="Некорректный номер рецепта"
        onRetry={() => window.location.assign('/')}
      />
    )
  }

  if (state.status === 'loading') {
    return <DetailsLoading />
  }

  if (state.status === 'error') {
    return <DetailsError message={state.message} onRetry={() => load()} />
  }

  const recipe = state.recipe

  return (
    <main className="mx-auto w-full max-w-6xl px-5 py-8 sm:px-8 sm:py-12">
      <div className="mb-7 flex flex-wrap items-center justify-between gap-3">
        <Button asChild variant="ghost">
          <Link to="/#recipes">
            <ArrowLeft aria-hidden="true" data-icon="inline-start" />
            Вернуться в каталог
          </Link>
        </Button>
        <div className="flex flex-wrap gap-2">
          <Button
            disabled={favoritePending}
            onClick={toggleFavorite}
            size="sm"
            variant={recipe.favorite ? 'secondary' : 'outline'}
          >
            <Heart
              aria-hidden="true"
              className={recipe.favorite ? 'fill-current' : undefined}
              data-icon="inline-start"
            />
            {favoritePending
              ? 'Сохраняем...'
              : recipe.favorite
                ? 'В избранном'
                : 'Добавить в избранное'}
          </Button>
          <Button asChild size="sm" variant="outline">
            <Link to={`/recipes/${recipe.id}/cookings/new`}>
              <NotebookPen aria-hidden="true" data-icon="inline-start" />
              Отметить приготовленным
            </Link>
          </Button>
          <Button asChild size="sm" variant="outline">
            <a href={backendUrl(`/recipes/${recipe.id}/edit`)}>
              <Pencil aria-hidden="true" data-icon="inline-start" />
              Редактировать
            </a>
          </Button>
        </div>
      </div>

      {favoriteError && (
        <p className="mb-5 text-sm text-destructive" role="alert">
          {favoriteError}
        </p>
      )}

      <section className="grid overflow-hidden rounded-3xl border bg-card shadow-sm lg:grid-cols-[1.05fr_0.95fr]">
        <div className="flex flex-col justify-center p-7 sm:p-10 lg:p-12">
          <div className="mb-5 flex flex-wrap gap-2">
            <Badge>{recipe.status}</Badge>
            <Badge variant="secondary">{recipe.cuisine}</Badge>
            <Badge variant="outline">{recipe.origin}</Badge>
            {recipe.favorite && (
              <Badge className="gap-1" variant="secondary">
                <Heart aria-hidden="true" className="size-3.5 fill-current" />
                В избранном
              </Badge>
            )}
          </div>
          <h1 className="font-heading text-4xl leading-tight font-semibold tracking-tight sm:text-5xl">
            {recipe.name}
          </h1>
          {recipe.englishName && (
            <p className="mt-2 text-lg text-muted-foreground">{recipe.englishName}</p>
          )}
          <p className="mt-6 max-w-2xl text-lg leading-8">{recipe.description}</p>
        </div>

        <div className="aspect-[4/3] min-h-72 bg-secondary lg:aspect-auto">
          {recipe.coverUrl ? (
            <img
              alt={`Готовое блюдо: ${recipe.name}`}
              className="size-full object-cover"
              src={backendUrl(recipe.coverUrl)}
            />
          ) : (
            <div className="grid size-full place-items-center text-primary">
              <ChefHat aria-hidden="true" className="size-20" />
            </div>
          )}
        </div>
      </section>

      {recipe.restrictionWarnings.length > 0 && (
        <RestrictionNotice recipe={recipe} />
      )}

      <dl className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Fact label="Подготовка" value={`${recipe.preparationMinutes} мин`} />
        <Fact label="Приготовление" value={`${recipe.cookingMinutes} мин`} />
        <Fact label="Общее время" value={`${recipe.totalMinutes} мин`} />
        <Fact label="Сложность" value={recipe.difficulty} />
      </dl>

      <div className="mt-6 grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <Card>
          <CardHeader className="gap-5 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="font-heading text-2xl font-semibold">Ингредиенты</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {recipe.servings === recipe.baseServings
                  ? `Количество указано на ${recipe.baseServings} порц.`
                  : `Пересчитано на ${recipe.servings} порц. Базовый рецепт — на ${recipe.baseServings}.`}
              </p>
            </div>
            <form className="flex items-end gap-2" onSubmit={recalculate}>
              <label className="grid gap-1 text-sm font-medium">
                Порций
                <input
                  className="h-9 w-20 rounded-lg border bg-background px-3 text-sm"
                  max="100"
                  min="1"
                  onChange={(event) => setServings(event.target.value)}
                  required
                  type="number"
                  value={servings}
                />
              </label>
              <Button size="sm" type="submit" variant="outline">
                <RotateCcw aria-hidden="true" data-icon="inline-start" />
                Пересчитать
              </Button>
            </form>
          </CardHeader>
          <CardContent>
            <ul className="divide-y">
              {recipe.ingredients.map((ingredient) => (
                <li className="py-4 first:pt-0 last:pb-0" key={ingredient.name}>
                  <div className="flex items-start justify-between gap-4">
                    <span>
                      {ingredient.name}
                      {ingredient.optional && (
                        <small className="ml-2 text-muted-foreground">необязательно</small>
                      )}
                    </span>
                    <strong className="whitespace-nowrap">
                      {ingredient.amount} {ingredient.unit}
                    </strong>
                  </div>
                  {ingredient.substitutions.length > 0 && (
                    <ul className="mt-3 space-y-2 rounded-xl bg-secondary/55 p-3 text-sm">
                      {ingredient.substitutions.map((substitution) => (
                        <li key={`${ingredient.name}-${substitution.ingredientName}`}>
                          <span>
                            Можно заменить на <strong>{substitution.ingredientName}</strong>,{' '}
                            {substitution.amount} {substitution.unit}
                          </span>
                          <small className="mt-1 block text-muted-foreground">
                            {substitution.type}. {substitution.impactNote}
                          </small>
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <h2 className="font-heading text-2xl font-semibold">Приготовление</h2>
          </CardHeader>
          <CardContent>
            <ol className="space-y-5">
              {recipe.steps.map((step) => (
                <li className="grid grid-cols-[2.5rem_1fr] gap-4" key={step.number}>
                  <span className="grid size-10 place-items-center rounded-full bg-primary font-semibold text-primary-foreground">
                    {step.number}
                  </span>
                  <div>
                    <p className="pt-2 leading-7">{step.instruction}</p>
                    {step.imageUrl && (
                      <img
                        alt={`Иллюстрация шага ${step.number}`}
                        className="mt-4 max-h-80 w-full rounded-2xl object-cover"
                        loading="lazy"
                        src={backendUrl(step.imageUrl)}
                      />
                    )}
                  </div>
                </li>
              ))}
            </ol>
          </CardContent>
        </Card>
      </div>

      <div className="mt-6 grid gap-6 md:grid-cols-2">
        <TagCard title="Оборудование" values={recipe.equipment} />
        <TagCard title="Способ приготовления" values={recipe.cookingMethods} />
      </div>

      {(recipe.personalNotes || recipe.videoUrl || recipe.sourceName) && (
        <Card className="mt-6">
          <CardContent className="grid gap-5 md:grid-cols-2">
            {recipe.personalNotes && (
              <div>
                <h2 className="font-heading text-xl font-semibold">Личная заметка</h2>
                <p className="mt-2 leading-7 text-muted-foreground">
                  {recipe.personalNotes}
                </p>
              </div>
            )}
            <div className="flex flex-col items-start gap-3">
              {recipe.videoUrl && (
                <Button asChild variant="outline">
                  <a href={recipe.videoUrl} rel="noreferrer" target="_blank">
                    <Play aria-hidden="true" data-icon="inline-start" />
                    Посмотреть видео приготовления
                  </a>
                </Button>
              )}
              {recipe.sourceName && (
                <p className="text-sm text-muted-foreground">
                  Источник:{' '}
                  {recipe.sourceUrl ? (
                    <a
                      className="font-medium text-primary underline underline-offset-4"
                      href={recipe.sourceUrl}
                      rel="noreferrer"
                      target="_blank"
                    >
                      {recipe.sourceName}
                      <ExternalLink aria-hidden="true" className="ml-1 inline size-3.5" />
                    </a>
                  ) : (
                    recipe.sourceName
                  )}
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      )}
    </main>
  )
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border bg-card p-5 shadow-sm">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="mt-2 flex items-center gap-2 font-heading text-xl font-semibold">
        {label === 'Общее время' && <Clock3 aria-hidden="true" className="size-5 text-primary" />}
        {value}
      </dd>
    </div>
  )
}

function TagCard({ title, values }: { title: string; values: string[] }) {
  return (
    <Card>
      <CardHeader>
        <h2 className="font-heading text-base font-medium">{title}</h2>
      </CardHeader>
      <CardContent className="flex flex-wrap gap-2">
        {values.map((value) => (
          <Badge key={value} variant="secondary">
            {value}
          </Badge>
        ))}
      </CardContent>
    </Card>
  )
}

function RestrictionNotice({ recipe }: { recipe: RecipeDetails }) {
  return (
    <section
      className={`mt-6 rounded-2xl border p-5 ${
        recipe.excludedByRestriction
          ? 'border-destructive/35 bg-destructive/5'
          : 'border-amber-500/35 bg-amber-50'
      }`}
    >
      <h2 className="flex items-center gap-2 font-heading text-xl font-semibold">
        <AlertTriangle aria-hidden="true" className="size-5" />
        {recipe.excludedByRestriction
          ? 'Рецепт исключён личным правилом'
          : 'Обрати внимание на ограничения'}
      </h2>
      <ul className="mt-3 space-y-2 text-sm">
        {recipe.restrictionWarnings.map((warning) => (
          <li key={`${warning.ingredientName}-${warning.modeName}`}>
            <strong>{warning.ingredientName}: {warning.modeName}</strong>
            {warning.optional && ' — необязательный ингредиент'}
            {warning.note && ` — ${warning.note}`}
          </li>
        ))}
      </ul>
    </section>
  )
}

function DetailsLoading() {
  return (
    <main aria-live="polite" className="mx-auto w-full max-w-6xl px-5 py-12 sm:px-8">
      <span className="sr-only">Загружаем рецепт</span>
      <div className="h-96 animate-pulse rounded-3xl border bg-card" />
    </main>
  )
}

function DetailsError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <main className="mx-auto w-full max-w-2xl px-5 py-20 text-center sm:px-8">
      <AlertTriangle aria-hidden="true" className="mx-auto size-12 text-destructive" />
      <h1 className="mt-5 font-heading text-3xl font-semibold">Рецепт не загрузился</h1>
      <p className="mt-3 text-muted-foreground" role="alert">{message}</p>
      <div className="mt-6 flex justify-center gap-3">
        <Button onClick={onRetry}>
          <RefreshCw aria-hidden="true" data-icon="inline-start" />
          Повторить
        </Button>
        <Button asChild variant="outline">
          <Link to="/">Вернуться в каталог</Link>
        </Button>
      </div>
    </main>
  )
}
