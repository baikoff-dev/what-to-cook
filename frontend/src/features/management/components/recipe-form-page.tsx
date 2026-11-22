import { type FormEvent, type ReactNode, useEffect, useState } from 'react'
import {
  ArrowLeft,
  ChefHat,
  CirclePlus,
  LoaderCircle,
  RefreshCw,
  Save,
  Trash2,
} from 'lucide-react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  createRecipe,
  loadRecipeForEditing,
  loadRecipeFormOptions,
  RecipeManagementApiError,
  updateRecipe,
  type RecipeEditResponse,
  type RecipeFormOptions,
  type RecipeInput,
} from '@/features/management/api/recipe-management'
import { backendUrl } from '@/lib/backend-url'

const fieldClass =
  'mt-2 h-11 w-full rounded-lg border bg-background px-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20'
const textAreaClass =
  'mt-2 min-h-28 w-full rounded-lg border bg-background p-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20'

interface IngredientDraft {
  name: string
  amount: string
  unit: string
  optional: boolean
}

interface SubstitutionDraft {
  originalIngredientName: string
  substituteIngredientName: string
  amount: string
  unit: string
  type: string
  impactNote: string
}

interface StepDraft {
  id: number | null
  instruction: string
  imageUrl: string | null
}

interface RecipeDraft {
  version: number
  nameRu: string
  nameEn: string
  description: string
  servings: string
  preparationMinutes: string
  cookingMinutes: string
  difficulty: string
  status: string
  cuisineId: string
  origin: string
  sourceName: string
  sourceUrl: string
  videoUrl: string
  personalNotes: string
  equipmentIds: number[]
  cookingMethodIds: number[]
  ingredients: IngredientDraft[]
  substitutions: SubstitutionDraft[]
  steps: StepDraft[]
  mainImageUrl: string | null
}

type OptionsState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'success'; options: RecipeFormOptions }

export function RecipeCreatePage() {
  return <RecipeFormPage />
}

export function RecipeEditPage() {
  const { recipeId } = useParams()
  const parsedRecipeId = Number(recipeId)

  if (!Number.isInteger(parsedRecipeId) || parsedRecipeId <= 0) {
    return <Navigate replace to="/" />
  }

  return <RecipeFormPage recipeId={parsedRecipeId} />
}

function RecipeFormPage({ recipeId }: { recipeId?: number }) {
  const navigate = useNavigate()
  const [optionsState, setOptionsState] = useState<OptionsState>({ status: 'loading' })
  const [draft, setDraft] = useState<RecipeDraft | null>(null)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [requestNumber, setRequestNumber] = useState(0)

  useEffect(() => {
    let active = true

    Promise.all([
      loadRecipeFormOptions(),
      recipeId ? loadRecipeForEditing(recipeId) : Promise.resolve(null),
    ])
      .then(([options, recipe]) => {
        if (active) {
          setOptionsState({ status: 'success', options })
          setDraft(recipe ? draftFromResponse(recipe) : emptyRecipeDraft(options))
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setOptionsState({
            status: 'error',
            message:
              requestError instanceof Error
                ? requestError.message
                : 'Не удалось загрузить форму рецепта',
          })
        }
      })

    return () => {
      active = false
    }
  }, [recipeId, requestNumber])

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!draft || pending) {
      return
    }

    const clientErrors = validateDraft(draft)
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      setError('Проверь обязательные поля формы')
      focusFirstInvalidField(clientErrors)
      return
    }

    setPending(true)
    setError(null)
    setFieldErrors({})

    try {
      const input = toRecipeInput(draft)
      const result = recipeId
        ? await updateRecipe(recipeId, input)
        : await createRecipe(input)
      navigate(`/recipes/${result.id}`)
    } catch (requestError: unknown) {
      if (requestError instanceof RecipeManagementApiError) {
        setFieldErrors(requestError.fieldErrors)
        focusFirstInvalidField(requestError.fieldErrors)
      }
      setError(
        requestError instanceof Error ? requestError.message : 'Не удалось сохранить рецепт',
      )
    } finally {
      setPending(false)
    }
  }

  if (optionsState.status === 'loading' || !draft) {
    return <FormLoading />
  }

  if (optionsState.status === 'error') {
    return (
      <FormError
        message={optionsState.message}
        onRetry={() => {
          setOptionsState({ status: 'loading' })
          setRequestNumber((value) => value + 1)
        }}
      />
    )
  }

  const options = optionsState.options
  const returnTo = recipeId ? `/recipes/${recipeId}` : '/#recipes'

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-8 sm:px-8 sm:py-12">
      <Button asChild variant="ghost">
        <Link to={returnTo}>
          <ArrowLeft aria-hidden="true" data-icon="inline-start" />
          {recipeId ? 'Вернуться к рецепту' : 'Вернуться в каталог'}
        </Link>
      </Button>

      <header className="mt-7 max-w-3xl">
        <p className="mb-3 flex items-center gap-2 text-sm font-semibold tracking-[0.16em] text-primary uppercase">
          <ChefHat aria-hidden="true" className="size-4" />
          Личный каталог
        </p>
        <h1 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
          {recipeId ? 'Редактирование рецепта' : 'Новый рецепт'}
        </h1>
        <p className="mt-4 text-lg leading-8 text-muted-foreground">
          {recipeId
            ? 'Измени данные рецепта. Загруженные обложка и фотографии шагов сохранятся без изменений.'
            : 'Сохрани точные количества, оборудование и последовательность приготовления. Обложку и фотографии шагов можно будет добавить на следующем этапе.'}
        </p>
      </header>

      <form className="mt-8 grid gap-6" noValidate onSubmit={submit}>
        {draft.mainImageUrl && (
          <Card>
            <CardContent className="flex flex-col gap-4 pt-6 sm:flex-row sm:items-center">
              <img
                alt={`Текущая обложка рецепта ${draft.nameRu}`}
                className="aspect-video w-full rounded-xl object-cover sm:w-48"
                src={backendUrl(draft.mainImageUrl)}
              />
              <div>
                <p className="font-semibold">Текущая обложка сохранится</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Заменить или удалить её можно будет на следующем этапе работы с изображениями.
                </p>
              </div>
            </CardContent>
          </Card>
        )}
        <FormSection title="Основная информация">
          <div className="grid gap-5 md:grid-cols-2">
            <Field error={fieldErrors.nameRu} id="name-ru" label="Название на русском">
              <input
                className={fieldClass}
                id="name-ru"
                maxLength={200}
                onChange={(event) => updateDraft('nameRu', event.target.value)}
                value={draft.nameRu}
              />
            </Field>
            <Field error={fieldErrors.nameEn} id="name-en" label="Название на английском">
              <input
                className={fieldClass}
                id="name-en"
                maxLength={200}
                onChange={(event) => updateDraft('nameEn', event.target.value)}
                value={draft.nameEn}
              />
            </Field>
          </div>

          <Field error={fieldErrors.description} id="description" label="Описание">
            <textarea
              className={textAreaClass}
              id="description"
              maxLength={5000}
              onChange={(event) => updateDraft('description', event.target.value)}
              value={draft.description}
            />
          </Field>

          <div className="grid gap-5 sm:grid-cols-3">
            <NumberField
              error={fieldErrors.servings}
              id="servings"
              label="Порций"
              max={100}
              min={1}
              value={draft.servings}
              onChange={(value) => updateDraft('servings', value)}
            />
            <NumberField
              error={fieldErrors.preparationMinutes}
              id="preparation-minutes"
              label="Подготовка, минут"
              max={1440}
              min={0}
              value={draft.preparationMinutes}
              onChange={(value) => updateDraft('preparationMinutes', value)}
            />
            <NumberField
              error={fieldErrors.cookingMinutes}
              id="cooking-minutes"
              label="Приготовление, минут"
              max={1440}
              min={0}
              value={draft.cookingMinutes}
              onChange={(value) => updateDraft('cookingMinutes', value)}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            <SelectField
              error={fieldErrors.difficulty}
              id="difficulty"
              label="Сложность"
              options={options.difficulties}
              value={draft.difficulty}
              onChange={(value) => updateDraft('difficulty', value)}
            />
            <SelectField
              error={fieldErrors.status}
              id="status"
              label="Статус"
              options={options.statuses}
              value={draft.status}
              onChange={(value) => updateDraft('status', value)}
            />
            <Field error={fieldErrors.cuisineId} id="cuisine" label="Кухня">
              <select
                className={fieldClass}
                id="cuisine"
                onChange={(event) => updateDraft('cuisineId', event.target.value)}
                value={draft.cuisineId}
              >
                <option value="">Выбери кухню</option>
                {options.cuisines.map((option) => (
                  <option key={option.id} value={option.id}>{option.name}</option>
                ))}
              </select>
            </Field>
            <SelectField
              error={fieldErrors.origin}
              id="origin"
              label="Происхождение"
              options={options.origins}
              value={draft.origin}
              onChange={(value) => updateDraft('origin', value)}
            />
          </div>
        </FormSection>

        <FormSection title="Оборудование и способы приготовления">
          <ChoiceGroup
            error={fieldErrors.equipmentIds}
            label="Оборудование"
            options={options.equipment}
            selected={draft.equipmentIds}
            onToggle={(id) => toggleSelection('equipmentIds', id)}
          />
          <ChoiceGroup
            error={fieldErrors.cookingMethodIds}
            label="Способы приготовления"
            options={options.cookingMethods}
            selected={draft.cookingMethodIds}
            onToggle={(id) => toggleSelection('cookingMethodIds', id)}
          />
        </FormSection>

        <FormSection
          action={(
            <Button onClick={addIngredient} size="sm" type="button" variant="outline">
              <CirclePlus aria-hidden="true" data-icon="inline-start" />
              Добавить ингредиент
            </Button>
          )}
          title="Ингредиенты"
        >
          <div className="grid gap-4">
            {draft.ingredients.map((ingredient, index) => (
              <div className="rounded-xl border bg-muted/20 p-4" key={`ingredient-${index}`}>
                <div className="grid gap-4 md:grid-cols-[minmax(0,1.6fr)_minmax(8rem,0.7fr)_minmax(9rem,0.8fr)_auto] md:items-end">
                  <Field
                    error={fieldErrors[`ingredients[${index}].name`]}
                    id={`ingredient-${index}-name`}
                    label={`Ингредиент ${index + 1}`}
                  >
                    <input
                      className={fieldClass}
                      id={`ingredient-${index}-name`}
                      maxLength={150}
                      onChange={(event) => updateIngredient(index, 'name', event.target.value)}
                      value={ingredient.name}
                    />
                  </Field>
                  <NumberField
                    error={fieldErrors[`ingredients[${index}].amount`]}
                    id={`ingredient-${index}-amount`}
                    label={`Количество ингредиента ${index + 1}`}
                    min={0.001}
                    step="0.001"
                    value={ingredient.amount}
                    onChange={(value) => updateIngredient(index, 'amount', value)}
                  />
                  <SelectField
                    error={fieldErrors[`ingredients[${index}].unit`]}
                    id={`ingredient-${index}-unit`}
                    label={`Единица ингредиента ${index + 1}`}
                    options={options.measurementUnits}
                    value={ingredient.unit}
                    onChange={(value) => updateIngredient(index, 'unit', value)}
                  />
                  <Button
                    aria-label={`Удалить ингредиент ${index + 1}`}
                    disabled={draft.ingredients.length === 1}
                    onClick={() => removeIngredient(index)}
                    size="icon"
                    type="button"
                    variant="ghost"
                  >
                    <Trash2 aria-hidden="true" />
                  </Button>
                </div>
                <label className="mt-4 flex items-center gap-2 text-sm">
                  <input
                    checked={ingredient.optional}
                    onChange={(event) => updateIngredient(index, 'optional', event.target.checked)}
                    type="checkbox"
                  />
                  Необязательный ингредиент
                </label>
              </div>
            ))}
          </div>
        </FormSection>

        <FormSection
          action={(
            <Button onClick={addSubstitution} size="sm" type="button" variant="outline">
              <CirclePlus aria-hidden="true" data-icon="inline-start" />
              Добавить замену
            </Button>
          )}
          title="Допустимые замены"
        >
          {draft.substitutions.length === 0 ? (
            <p className="text-sm text-muted-foreground">Замены пока не добавлены.</p>
          ) : (
            <div className="grid gap-4">
              {draft.substitutions.map((substitution, index) => (
                <div className="rounded-xl border bg-muted/20 p-4" key={`substitution-${index}`}>
                  <div className="grid gap-4 md:grid-cols-2">
                    <Field
                      error={fieldErrors[`substitutions[${index}].originalIngredientName`]}
                      id={`substitution-${index}-original`}
                      label={`Заменяемый ингредиент ${index + 1}`}
                    >
                      <input
                        className={fieldClass}
                        id={`substitution-${index}-original`}
                        list="recipe-ingredient-names"
                        onChange={(event) => updateSubstitution(index, 'originalIngredientName', event.target.value)}
                        value={substitution.originalIngredientName}
                      />
                    </Field>
                    <Field
                      error={fieldErrors[`substitutions[${index}].substituteIngredientName`]}
                      id={`substitution-${index}-substitute`}
                      label={`Ингредиент для замены ${index + 1}`}
                    >
                      <input
                        className={fieldClass}
                        id={`substitution-${index}-substitute`}
                        onChange={(event) => updateSubstitution(index, 'substituteIngredientName', event.target.value)}
                        value={substitution.substituteIngredientName}
                      />
                    </Field>
                  </div>
                  <div className="mt-4 grid gap-4 md:grid-cols-3">
                    <NumberField
                      error={fieldErrors[`substitutions[${index}].amount`]}
                      id={`substitution-${index}-amount`}
                      label={`Количество замены ${index + 1}`}
                      min={0.001}
                      step="0.001"
                      value={substitution.amount}
                      onChange={(value) => updateSubstitution(index, 'amount', value)}
                    />
                    <SelectField
                      error={fieldErrors[`substitutions[${index}].unit`]}
                      id={`substitution-${index}-unit`}
                      label={`Единица замены ${index + 1}`}
                      options={options.measurementUnits}
                      value={substitution.unit}
                      onChange={(value) => updateSubstitution(index, 'unit', value)}
                    />
                    <SelectField
                      error={fieldErrors[`substitutions[${index}].type`]}
                      id={`substitution-${index}-type`}
                      label={`Тип замены ${index + 1}`}
                      options={options.substitutionTypes}
                      value={substitution.type}
                      onChange={(value) => updateSubstitution(index, 'type', value)}
                    />
                  </div>
                  <Field
                    error={fieldErrors[`substitutions[${index}].impactNote`]}
                    id={`substitution-${index}-impact`}
                    label={`Как изменится блюдо ${index + 1}`}
                  >
                    <textarea
                      className={textAreaClass}
                      id={`substitution-${index}-impact`}
                      maxLength={1000}
                      onChange={(event) => updateSubstitution(index, 'impactNote', event.target.value)}
                      value={substitution.impactNote}
                    />
                  </Field>
                  <Button
                    className="mt-3"
                    onClick={() => removeSubstitution(index)}
                    size="sm"
                    type="button"
                    variant="ghost"
                  >
                    <Trash2 aria-hidden="true" data-icon="inline-start" />
                    Удалить замену
                  </Button>
                </div>
              ))}
            </div>
          )}
          <datalist id="recipe-ingredient-names">
            {draft.ingredients.filter((item) => item.name.trim()).map((item) => (
              <option key={item.name} value={item.name} />
            ))}
          </datalist>
        </FormSection>

        <FormSection
          action={(
            <Button onClick={addStep} size="sm" type="button" variant="outline">
              <CirclePlus aria-hidden="true" data-icon="inline-start" />
              Добавить шаг
            </Button>
          )}
          title="Приготовление"
        >
          <div className="grid gap-4">
            {draft.steps.map((step, index) => (
              <div className="grid gap-3 rounded-xl border bg-muted/20 p-4 md:grid-cols-[1fr_auto] md:items-end" key={`step-${index}`}>
                <Field
                  error={fieldErrors[`steps[${index}].instruction`]}
                  id={`step-${index}-instruction`}
                  label={`Шаг ${index + 1}`}
                >
                  <textarea
                    className={textAreaClass}
                    id={`step-${index}-instruction`}
                    maxLength={5000}
                    onChange={(event) => updateStep(index, event.target.value)}
                    value={step.instruction}
                  />
                  {step.imageUrl && (
                    <p className="mt-2 text-sm text-muted-foreground">
                      Изображение этого шага сохранится.
                    </p>
                  )}
                </Field>
                <Button
                  aria-label={`Удалить шаг ${index + 1}`}
                  disabled={draft.steps.length === 1}
                  onClick={() => removeStep(index)}
                  size="icon"
                  type="button"
                  variant="ghost"
                >
                  <Trash2 aria-hidden="true" />
                </Button>
              </div>
            ))}
          </div>
        </FormSection>

        <FormSection title="Источник и личная заметка">
          <div className="grid gap-5 md:grid-cols-2">
            <Field error={fieldErrors.sourceName} id="source-name" label="Название источника">
              <input
                className={fieldClass}
                id="source-name"
                maxLength={300}
                onChange={(event) => updateDraft('sourceName', event.target.value)}
                value={draft.sourceName}
              />
            </Field>
            <Field error={fieldErrors.sourceUrl} id="source-url" label="Ссылка на источник">
              <input
                className={fieldClass}
                id="source-url"
                onChange={(event) => updateDraft('sourceUrl', event.target.value)}
                placeholder="https://"
                type="url"
                value={draft.sourceUrl}
              />
            </Field>
          </div>
          <Field error={fieldErrors.videoUrl} id="video-url" label="Видео приготовления">
            <input
              className={fieldClass}
              id="video-url"
              onChange={(event) => updateDraft('videoUrl', event.target.value)}
              placeholder="https://"
              type="url"
              value={draft.videoUrl}
            />
          </Field>
          <Field error={fieldErrors.personalNotes} id="personal-notes" label="Личная заметка">
            <textarea
              className={textAreaClass}
              id="personal-notes"
              maxLength={5000}
              onChange={(event) => updateDraft('personalNotes', event.target.value)}
              value={draft.personalNotes}
            />
          </Field>
        </FormSection>

        {error && (
          <p className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive" role="alert">
            {error}
          </p>
        )}

        <div className="sticky bottom-4 flex flex-wrap gap-3 rounded-2xl border bg-background/95 p-4 shadow-lg backdrop-blur">
          <Button disabled={pending} type="submit">
            {pending ? (
              <LoaderCircle aria-hidden="true" className="animate-spin" />
            ) : (
              <Save aria-hidden="true" data-icon="inline-start" />
            )}
            {pending
              ? 'Сохраняем...'
              : recipeId
                ? 'Сохранить изменения'
                : 'Сохранить рецепт'}
          </Button>
          <Button asChild variant="outline">
            <Link to={returnTo}>Отмена</Link>
          </Button>
        </div>
      </form>
    </main>
  )

  function updateDraft<K extends keyof RecipeDraft>(field: K, value: RecipeDraft[K]) {
    setDraft((current) => current ? { ...current, [field]: value } : current)
  }

  function toggleSelection(field: 'equipmentIds' | 'cookingMethodIds', id: number) {
    setDraft((current) => {
      if (!current) return current
      const selected = current[field]
      return {
        ...current,
        [field]: selected.includes(id)
          ? selected.filter((item) => item !== id)
          : [...selected, id],
      }
    })
  }

  function addIngredient() {
    setDraft((current) => current ? {
      ...current,
      ingredients: [...current.ingredients, emptyIngredient(options)],
    } : current)
  }

  function updateIngredient<K extends keyof IngredientDraft>(
    index: number,
    field: K,
    value: IngredientDraft[K],
  ) {
    setDraft((current) => current ? {
      ...current,
      ingredients: current.ingredients.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [field]: value } : item,
      ),
    } : current)
  }

  function removeIngredient(index: number) {
    setDraft((current) => current ? {
      ...current,
      ingredients: current.ingredients.filter((_, itemIndex) => itemIndex !== index),
    } : current)
  }

  function addSubstitution() {
    setDraft((current) => current ? {
      ...current,
      substitutions: [...current.substitutions, emptySubstitution(options)],
    } : current)
  }

  function updateSubstitution<K extends keyof SubstitutionDraft>(
    index: number,
    field: K,
    value: SubstitutionDraft[K],
  ) {
    setDraft((current) => current ? {
      ...current,
      substitutions: current.substitutions.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [field]: value } : item,
      ),
    } : current)
  }

  function removeSubstitution(index: number) {
    setDraft((current) => current ? {
      ...current,
      substitutions: current.substitutions.filter((_, itemIndex) => itemIndex !== index),
    } : current)
  }

  function addStep() {
    setDraft((current) => current ? {
      ...current,
      steps: [...current.steps, { id: null, instruction: '', imageUrl: null }],
    } : current)
  }

  function updateStep(index: number, instruction: string) {
    setDraft((current) => current ? {
      ...current,
      steps: current.steps.map((item, itemIndex) =>
        itemIndex === index ? { ...item, instruction } : item,
      ),
    } : current)
  }

  function removeStep(index: number) {
    setDraft((current) => current ? {
      ...current,
      steps: current.steps.filter((_, itemIndex) => itemIndex !== index),
    } : current)
  }
}

function FormSection({
  action,
  children,
  title,
}: {
  action?: ReactNode
  children: ReactNode
  title: string
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-4">
        <CardTitle>{title}</CardTitle>
        {action}
      </CardHeader>
      <CardContent className="grid gap-5">{children}</CardContent>
    </Card>
  )
}

function Field({
  children,
  error,
  id,
  label,
}: {
  children: ReactNode
  error?: string
  id: string
  label: string
}) {
  return (
    <div>
      <label className="text-sm font-semibold" htmlFor={id}>{label}</label>
      {children}
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
    </div>
  )
}

function NumberField({
  error,
  id,
  label,
  max,
  min,
  onChange,
  step = '1',
  value,
}: {
  error?: string
  id: string
  label: string
  max?: number
  min: number
  onChange: (value: string) => void
  step?: string
  value: string
}) {
  return (
    <Field error={error} id={id} label={label}>
      <input
        className={fieldClass}
        id={id}
        max={max}
        min={min}
        onChange={(event) => onChange(event.target.value)}
        step={step}
        type="number"
        value={value}
      />
    </Field>
  )
}

function SelectField({
  error,
  id,
  label,
  onChange,
  options,
  value,
}: {
  error?: string
  id: string
  label: string
  onChange: (value: string) => void
  options: Array<{ value: string; name: string }>
  value: string
}) {
  return (
    <Field error={error} id={id} label={label}>
      <select
        className={fieldClass}
        id={id}
        onChange={(event) => onChange(event.target.value)}
        value={value}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>{option.name}</option>
        ))}
      </select>
    </Field>
  )
}

function ChoiceGroup({
  error,
  label,
  onToggle,
  options,
  selected,
}: {
  error?: string
  label: string
  onToggle: (id: number) => void
  options: Array<{ id: number; name: string }>
  selected: number[]
}) {
  return (
    <fieldset>
      <legend className="text-sm font-semibold">{label}</legend>
      <div className="mt-3 flex flex-wrap gap-3">
        {options.map((option) => (
          <label className="flex items-center gap-2 rounded-lg border bg-background px-3 py-2 text-sm" key={option.id}>
            <input
              checked={selected.includes(option.id)}
              onChange={() => onToggle(option.id)}
              type="checkbox"
            />
            {option.name}
          </label>
        ))}
      </div>
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
    </fieldset>
  )
}

function FormLoading() {
  return (
    <main className="mx-auto grid min-h-[55vh] w-full max-w-3xl place-items-center px-5 py-12">
      <div className="text-center">
        <LoaderCircle aria-hidden="true" className="mx-auto size-10 animate-spin text-primary" />
        <p className="mt-4 text-muted-foreground">Загружаем форму рецепта...</p>
      </div>
    </main>
  )
}

function FormError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <main className="mx-auto w-full max-w-2xl px-5 py-20 text-center sm:px-8">
      <h1 className="font-heading text-3xl font-semibold">Форма не загрузилась</h1>
      <p className="mt-3 text-muted-foreground" role="alert">{message}</p>
      <Button className="mt-6" onClick={onRetry}>
        <RefreshCw aria-hidden="true" data-icon="inline-start" />
        Повторить
      </Button>
    </main>
  )
}

function emptyRecipeDraft(options: RecipeFormOptions): RecipeDraft {
  return {
    version: 0,
    nameRu: '',
    nameEn: '',
    description: '',
    servings: '1',
    preparationMinutes: '0',
    cookingMinutes: '0',
    difficulty: optionValue(options.difficulties, 'EASY'),
    status: optionValue(options.statuses, 'UNTESTED'),
    cuisineId: options.cuisines[0] ? String(options.cuisines[0].id) : '',
    origin: optionValue(options.origins, 'PERSONAL'),
    sourceName: '',
    sourceUrl: '',
    videoUrl: '',
    personalNotes: '',
    equipmentIds: [],
    cookingMethodIds: [],
    ingredients: [emptyIngredient(options)],
    substitutions: [],
    steps: [{ id: null, instruction: '', imageUrl: null }],
    mainImageUrl: null,
  }
}

function draftFromResponse(recipe: RecipeEditResponse): RecipeDraft {
  return {
    version: recipe.version,
    nameRu: recipe.nameRu,
    nameEn: recipe.nameEn ?? '',
    description: recipe.description,
    servings: String(recipe.servings),
    preparationMinutes: String(recipe.preparationMinutes),
    cookingMinutes: String(recipe.cookingMinutes),
    difficulty: recipe.difficulty,
    status: recipe.status,
    cuisineId: String(recipe.cuisineId),
    origin: recipe.origin,
    sourceName: recipe.sourceName ?? '',
    sourceUrl: recipe.sourceUrl ?? '',
    videoUrl: recipe.videoUrl ?? '',
    personalNotes: recipe.personalNotes ?? '',
    equipmentIds: recipe.equipmentIds,
    cookingMethodIds: recipe.cookingMethodIds,
    ingredients: recipe.ingredients.map((ingredient) => ({
      ...ingredient,
      amount: String(ingredient.amount),
    })),
    substitutions: recipe.substitutions.map((substitution) => ({
      ...substitution,
      amount: String(substitution.amount),
    })),
    steps: recipe.steps.map((step) => ({
      id: step.id,
      instruction: step.instruction,
      imageUrl: step.imageUrl,
    })),
    mainImageUrl: recipe.mainImageUrl,
  }
}

function emptyIngredient(options: RecipeFormOptions): IngredientDraft {
  return {
    name: '',
    amount: '',
    unit: optionValue(options.measurementUnits, 'GRAM'),
    optional: false,
  }
}

function emptySubstitution(options: RecipeFormOptions): SubstitutionDraft {
  return {
    originalIngredientName: '',
    substituteIngredientName: '',
    amount: '',
    unit: optionValue(options.measurementUnits, 'GRAM'),
    type: optionValue(options.substitutionTypes, 'CLOSE'),
    impactNote: '',
  }
}

function optionValue(options: Array<{ value: string }>, preferred: string) {
  return options.some((option) => option.value === preferred)
    ? preferred
    : options[0]?.value ?? ''
}

function validateDraft(draft: RecipeDraft) {
  const errors: Record<string, string> = {}
  if (!draft.nameRu.trim()) errors.nameRu = 'Укажи русское название'
  if (!draft.description.trim()) errors.description = 'Добавь описание'
  if (!draft.cuisineId) errors.cuisineId = 'Выбери кухню'
  if (draft.equipmentIds.length === 0) errors.equipmentIds = 'Выбери оборудование'
  if (draft.cookingMethodIds.length === 0) errors.cookingMethodIds = 'Выбери способ приготовления'

  draft.ingredients.forEach((ingredient, index) => {
    if (!ingredient.name.trim()) errors[`ingredients[${index}].name`] = 'Укажи ингредиент'
    if (!positiveNumber(ingredient.amount)) {
      errors[`ingredients[${index}].amount`] = 'Количество должно быть больше нуля'
    }
  })
  draft.substitutions.forEach((substitution, index) => {
    if (!substitution.originalIngredientName.trim()) {
      errors[`substitutions[${index}].originalIngredientName`] = 'Укажи заменяемый ингредиент'
    }
    if (!substitution.substituteIngredientName.trim()) {
      errors[`substitutions[${index}].substituteIngredientName`] = 'Укажи ингредиент для замены'
    }
    if (!positiveNumber(substitution.amount)) {
      errors[`substitutions[${index}].amount`] = 'Количество должно быть больше нуля'
    }
    if (!substitution.impactNote.trim()) {
      errors[`substitutions[${index}].impactNote`] = 'Опиши влияние замены'
    }
  })
  draft.steps.forEach((step, index) => {
    if (!step.instruction.trim()) errors[`steps[${index}].instruction`] = 'Опиши шаг'
  })
  return errors
}

function toRecipeInput(draft: RecipeDraft): RecipeInput {
  return {
    version: draft.version,
    nameRu: draft.nameRu,
    nameEn: draft.nameEn,
    description: draft.description,
    servings: Number(draft.servings),
    preparationMinutes: Number(draft.preparationMinutes),
    cookingMinutes: Number(draft.cookingMinutes),
    difficulty: draft.difficulty,
    status: draft.status,
    cuisineId: Number(draft.cuisineId),
    origin: draft.origin,
    sourceName: draft.sourceName,
    sourceUrl: draft.sourceUrl,
    videoUrl: draft.videoUrl,
    personalNotes: draft.personalNotes,
    equipmentIds: draft.equipmentIds,
    cookingMethodIds: draft.cookingMethodIds,
    ingredients: draft.ingredients.map((ingredient) => ({
      ...ingredient,
      amount: Number(ingredient.amount),
    })),
    substitutions: draft.substitutions.map((substitution) => ({
      ...substitution,
      amount: Number(substitution.amount),
    })),
    steps: draft.steps.map((step) => ({
      id: step.id,
      instruction: step.instruction,
    })),
  }
}

function positiveNumber(value: string) {
  const number = Number(value)
  return Number.isFinite(number) && number > 0
}

function focusFirstInvalidField(errors: Record<string, string>) {
  const firstField = Object.keys(errors)[0]
  if (!firstField) return
  window.setTimeout(() => {
    const invalidElement = document.querySelector<HTMLElement>(`[data-field="${firstField}"]`)
    invalidElement?.focus()
  })
}
