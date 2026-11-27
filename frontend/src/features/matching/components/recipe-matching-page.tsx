import { type FormEvent, useEffect, useState } from 'react'
import { ChefHat, Clock3, RefreshCw, RotateCcw, Search } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import {
  loadRecipeMatches,
  type RecipeMatchingPageData,
} from '@/features/matching/api/recipe-matching'
import { backendUrl } from '@/lib/backend-url'

type MatchingState =
  | { status: 'loading' }
  | { status: 'success'; data: RecipeMatchingPageData }
  | { status: 'error' }

export function RecipeMatchingPage() {
  const [state, setState] = useState<MatchingState>({ status: 'loading' })
  const [selectedIngredientIds, setSelectedIngredientIds] = useState<number[]>([])
  const [productQuery, setProductQuery] = useState('')
  const [lastRequestedIngredientIds, setLastRequestedIngredientIds] = useState<
    number[]
  >([])

  useEffect(() => {
    let active = true

    loadRecipeMatches()
      .then((data) => {
        if (active) {
          setSelectedIngredientIds(data.selectedIngredientIds)
          setState({ status: 'success', data })
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
  }, [])

  const toggleIngredient = (ingredientId: number) => {
    setSelectedIngredientIds((currentIds) =>
      currentIds.includes(ingredientId)
        ? currentIds.filter((id) => id !== ingredientId)
        : [...currentIds, ingredientId],
    )
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await requestMatches(selectedIngredientIds)
  }

  const requestMatches = async (ingredientIds: number[]) => {
    setLastRequestedIngredientIds(ingredientIds)
    setState({ status: 'loading' })

    try {
      const data = await loadRecipeMatches(ingredientIds)
      setSelectedIngredientIds(data.selectedIngredientIds)
      setState({ status: 'success', data })
    } catch {
      setState({ status: 'error' })
    }
  }

  const retry = () => {
    void requestMatches(lastRequestedIngredientIds)
  }

  const clear = () => {
    setProductQuery('')
    setSelectedIngredientIds([])
    setLastRequestedIngredientIds([])
    setState((currentState) =>
      currentState.status === 'success'
        ? {
            status: 'success',
            data: {
              ...currentState.data,
              selectedIngredientIds: [],
              recipes: [],
            },
          }
        : currentState,
    )
  }

  const normalizedQuery = productQuery.trim().toLocaleLowerCase('ru-RU')
  const filteredIngredients =
    state.status === 'success'
      ? state.data.ingredients.filter((ingredient) =>
          ingredient.name.toLocaleLowerCase('ru-RU').includes(normalizedQuery),
        )
      : []

  return (
    <main className="mx-auto w-full max-w-6xl px-5 py-12 sm:px-8 sm:py-16">
      <Link className="text-sm font-medium text-primary hover:underline" to="/">
        ← Вернуться в каталог
      </Link>

      <section className="mt-6 max-w-3xl">
        <p className="text-sm font-semibold tracking-[0.16em] text-primary uppercase">
          Подбор по продуктам
        </p>
        <h1 className="mt-3 font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
          Что можно приготовить?
        </h1>
        <p className="mt-4 text-lg text-muted-foreground">
          Отметь продукты, которые есть дома. Количество пока не учитывается.
        </p>
      </section>

      {state.status === 'error' && (
        <div className="mt-8 rounded-2xl border border-destructive/30 bg-background p-6">
          <p className="font-semibold">Не удалось загрузить подбор по продуктам</p>
          <p className="mt-2 text-sm text-muted-foreground">
            Проверь, что backend запущен, и попробуй ещё раз.
          </p>
          <Button className="mt-5" onClick={retry} type="button" variant="outline">
            <RefreshCw aria-hidden="true" data-icon="inline-start" />
            Повторить
          </Button>
        </div>
      )}

      {state.status === 'loading' && (
        <p className="mt-8 text-sm text-muted-foreground">Загружаем продукты...</p>
      )}

      {state.status === 'success' && (
        <>
          <form
            className="mt-8 rounded-3xl border bg-background/90 p-6 shadow-sm sm:p-8"
            onSubmit={submit}
          >
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="font-heading text-2xl font-semibold">Продукты дома</h2>
              <Badge variant="secondary">
                Выбрано: {selectedIngredientIds.length}
              </Badge>
            </div>
            <label className="mt-6 grid max-w-xl gap-2 text-sm font-medium">
              Найти продукт в списке
              <span className="relative">
                <Search
                  aria-hidden="true"
                  className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
                />
                <input
                  className="h-10 w-full rounded-lg border bg-background pr-3 pl-9 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20"
                  onChange={(event) => setProductQuery(event.target.value)}
                  placeholder="Например, картофель"
                  type="search"
                  value={productQuery}
                />
              </span>
            </label>
            <div className="mt-6 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {filteredIngredients.map((ingredient) => (
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
              {filteredIngredients.length === 0 && (
                <p className="py-4 text-sm text-muted-foreground">
                  Продукты по запросу не найдены
                </p>
              )}
            </div>
            <div className="mt-6 flex flex-wrap gap-3">
              <Button disabled={selectedIngredientIds.length === 0} type="submit">
                Подобрать рецепты
              </Button>
              {(productQuery || selectedIngredientIds.length > 0) && (
                <Button onClick={clear} type="button" variant="outline">
                  <RotateCcw aria-hidden="true" data-icon="inline-start" />
                  Очистить
                </Button>
              )}
            </div>
          </form>

          {state.data.selectedIngredientIds.length > 0 && (
            <section className="mt-10" aria-labelledby="matching-results-title">
              <div className="mb-5 flex items-center justify-between gap-4">
                <h2
                  className="font-heading text-2xl font-semibold"
                  id="matching-results-title"
                >
                  Результаты подбора
                </h2>
                <Badge variant="secondary">Рецептов: {state.data.recipes.length}</Badge>
              </div>
              {state.data.recipes.length === 0 ? (
                <div className="rounded-2xl border bg-background p-8 text-center">
                  <Search aria-hidden="true" className="mx-auto size-9 text-primary" />
                  <p className="mt-4 font-semibold">
                    По выбранным продуктам рецепты не найдены. Попробуй выбрать
                    другие продукты.
                  </p>
                </div>
              ) : (
                <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                  {state.data.recipes.map((recipe) => (
                  <Card
                    className="overflow-hidden bg-background/90 py-0 shadow-sm"
                    key={recipe.id}
                  >
                    <div className="aspect-[4/3] overflow-hidden bg-secondary">
                      {recipe.coverUrl ? (
                        <img
                          alt={`Обложка блюда «${recipe.nameRu}»`}
                          className="size-full object-cover"
                          src={backendUrl(recipe.coverUrl)}
                        />
                      ) : (
                        <div className="grid size-full place-items-center text-primary">
                          <ChefHat aria-hidden="true" className="size-12" />
                        </div>
                      )}
                    </div>
                    <CardHeader className="gap-3 pt-5">
                      <Badge className="w-fit" variant="outline">
                        Совпадение: {recipe.matchPercentage}%
                      </Badge>
                      <h3 className="font-heading text-xl leading-tight font-semibold">
                        <Link className="hover:text-primary" to={`/recipes/${recipe.id}`}>
                          {recipe.nameRu}
                        </Link>
                      </h3>
                    </CardHeader>
                    <CardContent className="pb-5">
                      <p className="line-clamp-3 min-h-18 text-sm leading-6 text-muted-foreground">
                        {recipe.description}
                      </p>
                      <div className="mt-5 flex flex-wrap gap-4 text-sm font-medium">
                        <span className="flex items-center gap-1.5">
                          <Clock3 aria-hidden="true" className="size-4 text-primary" />
                          {recipe.totalMinutes} мин
                        </span>
                        <span>{recipe.difficulty}</span>
                      </div>
                      <div className="mt-5 space-y-4 border-t pt-4 text-sm">
                        <p className="font-medium">
                          {recipe.matchedIngredientCount} из{' '}
                          {recipe.requiredIngredientCount} обязательных продуктов
                        </p>
                        <MatchList
                          heading="Не хватает"
                          items={recipe.missingIngredients}
                        />
                        <MatchList
                          heading="Из базовой кладовой"
                          items={recipe.pantryIngredientNames}
                        />
                        {recipe.substitutionUsages.length > 0 && (
                          <section>
                            <h4 className="font-semibold">Допустимые замены</h4>
                            <div className="mt-2 space-y-3">
                              {recipe.substitutionUsages.map((usage) => (
                                <div
                                  className="rounded-lg bg-muted/60 p-3"
                                  key={`${usage.originalIngredientName}-${usage.substituteIngredientName}`}
                                >
                                  <p className="font-medium">
                                    {usage.originalIngredientName} →{' '}
                                    {usage.substituteIngredientName}
                                  </p>
                                  {usage.impactNote && (
                                    <p className="mt-1 text-muted-foreground">
                                      {usage.impactNote}
                                    </p>
                                  )}
                                </div>
                              ))}
                            </div>
                          </section>
                        )}
                        {recipe.restrictionWarnings.length > 0 && (
                          <section className="rounded-xl border border-amber-500/30 bg-amber-50/60 p-3 dark:bg-amber-950/20">
                            <h4 className="font-semibold">Личные ограничения</h4>
                            <div className="mt-2 space-y-2">
                              {recipe.restrictionWarnings.map((warning) => (
                                <div
                                  key={`${warning.ingredientName}-${warning.modeName}`}
                                >
                                  <p className="font-medium">
                                    {warning.ingredientName}: {warning.modeName}
                                  </p>
                                  {warning.note && (
                                    <p className="text-muted-foreground">
                                      {warning.note}
                                    </p>
                                  )}
                                </div>
                              ))}
                            </div>
                          </section>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                  ))}
                </div>
              )}
            </section>
          )}
        </>
      )}
    </main>
  )
}

function MatchList({ heading, items }: { heading: string; items: string[] }) {
  if (items.length === 0) {
    return null
  }

  return (
    <section>
      <h4 className="font-semibold">{heading}</h4>
      <ul className="mt-2 list-inside list-disc space-y-1 text-muted-foreground">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </section>
  )
}
