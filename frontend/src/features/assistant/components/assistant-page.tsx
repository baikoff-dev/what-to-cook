import { type FormEvent, useEffect, useState } from 'react'
import { ChefHat, Clock3, Search, Sparkles, X } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import {
  loadAssistantOptions,
  loadPreferenceRecommendations,
  loadSurpriseRecommendations,
  type AssistantOptions,
  type AssistantRecommendation,
} from '@/features/assistant/api/assistant-recommendations'
import { backendUrl } from '@/lib/backend-url'

type AssistantState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; items: AssistantRecommendation[] }
  | { status: 'error' }

type OptionsState =
  | { status: 'loading' }
  | { status: 'success'; options: AssistantOptions }
  | { status: 'error' }

interface PreferenceForm {
  ingredientIds: number[]
  maxMinutes: string
  equipmentId: string
  cookingMethodId: string
  maxDifficulty: string
  goal: string
}

const emptyPreferences: PreferenceForm = {
  ingredientIds: [],
  maxMinutes: '',
  equipmentId: '',
  cookingMethodId: '',
  maxDifficulty: '',
  goal: 'BALANCED',
}

const fieldClass =
  'h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20'

export function AssistantPage() {
  const [state, setState] = useState<AssistantState>({ status: 'idle' })
  const [optionsState, setOptionsState] = useState<OptionsState>({
    status: 'loading',
  })
  const [preferences, setPreferences] = useState<PreferenceForm>(emptyPreferences)
  const [productQuery, setProductQuery] = useState('')
  const [validationErrors, setValidationErrors] = useState<string[]>([])

  useEffect(() => {
    let active = true

    loadAssistantOptions()
      .then((options) => {
        if (active) {
          setOptionsState({ status: 'success', options })
        }
      })
      .catch(() => {
        if (active) {
          setOptionsState({ status: 'error' })
        }
      })

    return () => {
      active = false
    }
  }, [])

  const surpriseMe = async () => {
    setState({ status: 'loading' })
    try {
      const response = await loadSurpriseRecommendations()
      setState({ status: 'success', items: response.items })
    } catch {
      setState({ status: 'error' })
    }
  }

  const submitPreferences = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const errors = validatePreferences(preferences)
    if (errors.length > 0) {
      setValidationErrors(errors)
      return
    }

    setValidationErrors([])
    setState({ status: 'loading' })
    try {
      const response = await loadPreferenceRecommendations({
        ingredientIds: preferences.ingredientIds,
        maxMinutes: numberOrUndefined(preferences.maxMinutes),
        equipmentId: numberOrUndefined(preferences.equipmentId),
        cookingMethodId: numberOrUndefined(preferences.cookingMethodId),
        maxDifficulty: preferences.maxDifficulty || undefined,
        goal: preferences.goal,
      })
      setState({ status: 'success', items: response.items })
    } catch {
      setState({ status: 'error' })
    }
  }

  const toggleIngredient = (ingredientId: number) => {
    setPreferences((current) => ({
      ...current,
      ingredientIds: current.ingredientIds.includes(ingredientId)
        ? current.ingredientIds.filter((id) => id !== ingredientId)
        : [...current.ingredientIds, ingredientId],
    }))
  }

  const updatePreference = (
    name: Exclude<keyof PreferenceForm, 'ingredientIds'>,
    value: string,
  ) => {
    setPreferences((current) => ({ ...current, [name]: value }))
  }

  const resetAssistant = () => {
    setPreferences(emptyPreferences)
    setProductQuery('')
    setValidationErrors([])
    setState({ status: 'idle' })
  }

  const recommendations = state.status === 'success' ? state.items : []
  const hasRecommendations = recommendations.length > 0

  return (
    <main className="mx-auto w-full max-w-6xl px-5 py-12 sm:px-8 sm:py-16">
      <section className="overflow-hidden rounded-3xl border bg-card/80 p-6 shadow-sm sm:p-10">
        <Badge variant="outline">Помощник выбора блюда</Badge>
        <div className="mt-5 grid gap-7 lg:grid-cols-[1fr_auto] lg:items-end">
          <div className="max-w-3xl">
            <h1 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
              Не знаешь, что приготовить?
            </h1>
            <p className="mt-4 text-lg leading-8 text-muted-foreground">
              Помощник выберет до трёх блюд из твоего каталога и учтёт личные
              ограничения и историю приготовления.
            </p>
          </div>
          <Button
            disabled={state.status === 'loading'}
            onClick={surpriseMe}
            size="lg"
            type="button"
          >
            <Sparkles aria-hidden="true" data-icon="inline-start" />
            {state.status === 'loading'
              ? 'Подбираем...'
              : hasRecommendations
                ? 'Удиви ещё раз'
                : 'Не знаю, предложи сам'}
          </Button>
        </div>
      </section>

      <PreferenceSection
        onProductQueryChange={setProductQuery}
        onSubmit={submitPreferences}
        onToggleIngredient={toggleIngredient}
        onUpdate={updatePreference}
        optionsState={optionsState}
        preferences={preferences}
        productQuery={productQuery}
        submitting={state.status === 'loading'}
        validationErrors={validationErrors}
      />

      {state.status === 'error' && (
        <div className="mt-8 rounded-2xl border border-destructive/30 bg-background p-6 text-center">
          <p className="font-semibold">Не удалось получить рекомендации</p>
          <p className="mt-2 text-sm text-muted-foreground">
            Проверь, что backend запущен, и попробуй ещё раз.
          </p>
        </div>
      )}

      {state.status === 'success' && state.items.length === 0 && (
        <div className="mt-8 rounded-2xl border bg-background p-8 text-center">
          <p className="font-semibold">Подходящих блюд пока нет</p>
          <p className="mt-2 text-sm text-muted-foreground">
            Попробуй увеличить время, выбрать другую цель или убрать часть условий.
          </p>
          <Button className="mt-5" onClick={resetAssistant} type="button" variant="outline">
            Начать заново
          </Button>
        </div>
      )}

      {hasRecommendations && (
        <section className="mt-10">
          <div className="mb-5 flex items-center justify-between gap-4">
            <h2 className="font-heading text-2xl font-semibold">
              Попробуй один из этих вариантов
            </h2>
            <div className="flex items-center gap-3">
              <Badge variant="secondary">Вариантов: {recommendations.length}</Badge>
              <Button onClick={resetAssistant} type="button" variant="outline">
                Начать заново
              </Button>
            </div>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {recommendations.map((recipe) => (
              <RecommendationCard key={recipe.id} recipe={recipe} />
            ))}
          </div>
        </section>
      )}
    </main>
  )
}

function PreferenceSection({
  onProductQueryChange,
  onSubmit,
  onToggleIngredient,
  onUpdate,
  optionsState,
  preferences,
  productQuery,
  submitting,
  validationErrors,
}: {
  onProductQueryChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onToggleIngredient: (ingredientId: number) => void
  onUpdate: (
    name: Exclude<keyof PreferenceForm, 'ingredientIds'>,
    value: string,
  ) => void
  optionsState: OptionsState
  preferences: PreferenceForm
  productQuery: string
  submitting: boolean
  validationErrors: string[]
}) {
  return (
    <section className="mt-8 rounded-3xl border bg-background/90 p-6 shadow-sm sm:p-8">
      <div className="max-w-2xl">
        <h2 className="font-heading text-2xl font-semibold">Уточни пожелания</h2>
        <p className="mt-2 text-muted-foreground">
          Выбери продукты и условия — незаполненные параметры не ограничивают подбор.
        </p>
      </div>

      {optionsState.status === 'loading' && (
        <p className="mt-6 text-sm text-muted-foreground">Загружаем параметры...</p>
      )}
      {optionsState.status === 'error' && (
        <p className="mt-6 text-sm text-destructive">
          Не удалось загрузить параметры помощника.
        </p>
      )}
      {validationErrors.length > 0 && (
        <div
          className="mt-6 rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm"
          role="alert"
        >
          <p className="font-semibold">Проверь параметры подбора</p>
          <ul className="mt-2 list-inside list-disc space-y-1 text-destructive">
            {validationErrors.map((error) => (
              <li key={error}>{error}</li>
            ))}
          </ul>
        </div>
      )}
      {optionsState.status === 'success' && (
        <PreferenceFormContent
          onProductQueryChange={onProductQueryChange}
          onSubmit={onSubmit}
          onToggleIngredient={onToggleIngredient}
          onUpdate={onUpdate}
          options={optionsState.options}
          preferences={preferences}
          productQuery={productQuery}
          submitting={submitting}
        />
      )}
    </section>
  )
}

function PreferenceFormContent({
  onProductQueryChange,
  onSubmit,
  onToggleIngredient,
  onUpdate,
  options,
  preferences,
  productQuery,
  submitting,
}: {
  onProductQueryChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onToggleIngredient: (ingredientId: number) => void
  onUpdate: (
    name: Exclude<keyof PreferenceForm, 'ingredientIds'>,
    value: string,
  ) => void
  options: AssistantOptions
  preferences: PreferenceForm
  productQuery: string
  submitting: boolean
}) {
  const normalizedQuery = productQuery.trim().toLocaleLowerCase('ru-RU')
  const filteredIngredients = options.ingredients.filter((ingredient) =>
    ingredient.name.toLocaleLowerCase('ru-RU').includes(normalizedQuery),
  )
  const selectedIngredients = options.ingredients.filter((ingredient) =>
    preferences.ingredientIds.includes(ingredient.id),
  )

  return (
    <form className="mt-7" noValidate onSubmit={onSubmit}>
      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div>
          <label className="grid gap-2 text-sm font-medium">
            Поиск продуктов
            <span className="relative">
              <Search
                aria-hidden="true"
                className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
              />
              <input
                className={`${fieldClass} pl-9`}
                onChange={(event) => onProductQueryChange(event.target.value)}
                placeholder="Например, паприка"
                type="search"
                value={productQuery}
              />
            </span>
          </label>

          {selectedIngredients.length > 0 && (
            <div className="mt-4 flex flex-wrap gap-2">
              {selectedIngredients.map((ingredient) => (
                <Badge key={ingredient.id} variant="secondary">
                  {ingredient.name}
                  <button
                    aria-label={`Убрать продукт: ${ingredient.name}`}
                    className="ml-1 rounded-full"
                    onClick={() => onToggleIngredient(ingredient.id)}
                    type="button"
                  >
                    <X aria-hidden="true" className="size-3" />
                  </button>
                </Badge>
              ))}
            </div>
          )}

          <div className="mt-4 max-h-52 space-y-2 overflow-y-auto rounded-xl border p-3">
            {filteredIngredients.map((ingredient) => (
              <label
                className="flex cursor-pointer items-center gap-3 rounded-lg px-2 py-2 hover:bg-muted"
                key={ingredient.id}
              >
                <input
                  checked={preferences.ingredientIds.includes(ingredient.id)}
                  onChange={() => onToggleIngredient(ingredient.id)}
                  type="checkbox"
                />
                <span className="text-sm">{ingredient.name}</span>
              </label>
            ))}
            {filteredIngredients.length === 0 && (
              <p className="py-3 text-center text-sm text-muted-foreground">
                Продукты не найдены
              </p>
            )}
          </div>
        </div>

        <div className="grid content-start gap-4 sm:grid-cols-2">
          <label className="grid gap-2 text-sm font-medium">
            Не дольше, минут
            <input
              className={fieldClass}
              max="1440"
              min="1"
              onChange={(event) => onUpdate('maxMinutes', event.target.value)}
              placeholder="Любое время"
              type="number"
              value={preferences.maxMinutes}
            />
          </label>
          <PreferenceSelect
            label="Максимальная сложность"
            onChange={(value) => onUpdate('maxDifficulty', value)}
            options={options.difficulties}
            placeholder="Любая"
            value={preferences.maxDifficulty}
          />
          <PreferenceSelect
            label="Оборудование"
            onChange={(value) => onUpdate('equipmentId', value)}
            options={options.equipment.map((option) => ({
              value: String(option.id),
              name: option.name,
            }))}
            placeholder="Любое"
            value={preferences.equipmentId}
          />
          <PreferenceSelect
            label="Способ приготовления"
            onChange={(value) => onUpdate('cookingMethodId', value)}
            options={options.cookingMethods.map((option) => ({
              value: String(option.id),
              name: option.name,
            }))}
            placeholder="Любой"
            value={preferences.cookingMethodId}
          />
          <div className="sm:col-span-2">
            <PreferenceSelect
              label="Цель"
              onChange={(value) => onUpdate('goal', value)}
              options={options.goals}
              placeholder="Без разницы"
              value={preferences.goal}
            />
          </div>
        </div>
      </div>

      <Button className="mt-6" disabled={submitting} type="submit">
        <Search aria-hidden="true" data-icon="inline-start" />
        {submitting ? 'Подбираем...' : 'Подобрать блюдо'}
      </Button>
    </form>
  )
}

function PreferenceSelect({
  label,
  onChange,
  options,
  placeholder,
  value,
}: {
  label: string
  onChange: (value: string) => void
  options: Array<{ value: string; name: string }>
  placeholder: string
  value: string
}) {
  return (
    <label className="grid gap-2 text-sm font-medium">
      {label}
      <select
        className={fieldClass}
        onChange={(event) => onChange(event.target.value)}
        value={value}
      >
        <option value="">{placeholder}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.name}
          </option>
        ))}
      </select>
    </label>
  )
}

function RecommendationCard({ recipe }: { recipe: AssistantRecommendation }) {
  return (
    <Card className="overflow-hidden bg-background/90 py-0 shadow-sm">
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
      <CardHeader className="pt-5">
        <div className="flex flex-wrap gap-2">
          {recipe.favorite && <Badge variant="secondary">В избранном</Badge>}
          {recipe.ingredientsSpecified && (
            <Badge variant="outline">Совпадение: {recipe.matchPercentage}%</Badge>
          )}
        </div>
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
        {(recipe.averageRating !== null || recipe.lastCookedOn) && (
          <div className="mt-3 space-y-1 text-sm text-muted-foreground">
            {recipe.averageRating !== null && (
              <p>Средняя оценка: {formatRating(recipe.averageRating)} из 5</p>
            )}
            {recipe.lastCookedOn && <p>Последний раз: {formatDate(recipe.lastCookedOn)}</p>}
          </div>
        )}
        {(recipe.equipment.length > 0 || recipe.cookingMethods.length > 0) && (
          <div className="mt-4 space-y-1 text-sm">
            {recipe.equipment.length > 0 && (
              <p>Оборудование: {recipe.equipment.join(', ')}</p>
            )}
            {recipe.cookingMethods.length > 0 && (
              <p>Способ: {recipe.cookingMethods.join(', ')}</p>
            )}
          </div>
        )}
        {recipe.ingredientsSpecified && (
          <div className="mt-5 space-y-4 border-t pt-4 text-sm">
            <p className="font-medium">
              {recipe.matchedIngredientCount} из {recipe.requiredIngredientCount} обязательных продуктов
            </p>
            <RecommendationList heading="Не хватает" items={recipe.missingIngredients} />
            <RecommendationList
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
                        {usage.originalIngredientName} → {usage.substituteIngredientName}
                      </p>
                      {usage.impactNote && (
                        <p className="mt-1 text-muted-foreground">{usage.impactNote}</p>
                      )}
                    </div>
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
        <RecommendationList heading="Почему подходит" items={recipe.reasons} />
        {recipe.restrictionWarnings.length > 0 && (
          <section className="mt-5 rounded-xl border border-amber-500/30 bg-amber-50/60 p-3 text-sm dark:bg-amber-950/20">
            <h4 className="font-semibold">Личные ограничения</h4>
            <div className="mt-2 space-y-2">
              {recipe.restrictionWarnings.map((warning) => (
                <div key={`${warning.ingredientName}-${warning.modeName}`}>
                  <p className="font-medium">
                    {warning.ingredientName}: {warning.modeName}
                  </p>
                  {warning.note && <p className="text-muted-foreground">{warning.note}</p>}
                </div>
              ))}
            </div>
          </section>
        )}
      </CardContent>
    </Card>
  )
}

function RecommendationList({ heading, items }: { heading: string; items: string[] }) {
  if (items.length === 0) {
    return null
  }

  return (
    <section className="mt-5 text-sm">
      <h4 className="font-semibold">{heading}</h4>
      <ul className="mt-2 list-inside list-disc space-y-1 text-muted-foreground">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </section>
  )
}

function formatRating(value: number) {
  return value.toFixed(1).replace('.', ',')
}

function formatDate(value: string) {
  const [year, month, day] = value.split('-')
  return `${day}.${month}.${year}`
}

function numberOrUndefined(value: string) {
  return value ? Number(value) : undefined
}

function validatePreferences(preferences: PreferenceForm) {
  const errors: string[] = []
  const maxMinutes = numberOrUndefined(preferences.maxMinutes)

  if (maxMinutes !== undefined && maxMinutes < 1) {
    errors.push('Время должно быть не меньше 1 минуты')
  }
  if (maxMinutes !== undefined && maxMinutes > 1440) {
    errors.push('Время должно быть не больше 1440 минут')
  }
  if (
    preferences.goal === 'USE_PRODUCTS' &&
    preferences.ingredientIds.length === 0
  ) {
    errors.push(
      'Для цели «Использовать имеющиеся продукты» выбери хотя бы один продукт',
    )
  }

  return errors
}
