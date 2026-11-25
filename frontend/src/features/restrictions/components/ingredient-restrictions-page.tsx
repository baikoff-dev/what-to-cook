import { type FormEvent, useEffect, useState } from 'react'
import {
  AlertTriangle,
  ArrowLeft,
  Pencil,
  RefreshCw,
  ShieldAlert,
  Trash2,
} from 'lucide-react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import {
  IngredientRestrictionRequestError,
  deleteIngredientRestriction,
  loadIngredientRestrictions,
  saveIngredientRestriction,
  type IngredientRestrictionPage,
  type IngredientRestrictionRule,
  type RestrictionModeOption,
} from '@/features/restrictions/api/ingredient-restrictions'

type RestrictionsState =
  | { status: 'loading' }
  | { status: 'success'; page: IngredientRestrictionPage }
  | { status: 'error'; message: string }

type RestrictionDraft = {
  ingredientId: string
  mode: '' | RestrictionModeOption['value']
  note: string
}

const emptyDraft: RestrictionDraft = {
  ingredientId: '',
  mode: '',
  note: '',
}

export function IngredientRestrictionsPage() {
  const [state, setState] = useState<RestrictionsState>({ status: 'loading' })
  const [requestNumber, setRequestNumber] = useState(0)
  const [draft, setDraft] = useState<RestrictionDraft>(emptyDraft)
  const [editingIngredientId, setEditingIngredientId] = useState<number | null>(null)
  const [confirmingDeleteId, setConfirmingDeleteId] = useState<number | null>(null)
  const [deletingIngredientId, setDeletingIngredientId] = useState<number | null>(null)
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    let active = true

    loadIngredientRestrictions()
      .then((page) => {
        if (active) setState({ status: 'success', page })
      })
      .catch((error: unknown) => {
        if (active) {
          setState({
            status: 'error',
            message:
              error instanceof Error
                ? error.message
                : 'Не удалось загрузить ограничения по ингредиентам',
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

  const saveRule = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const errors: Record<string, string> = {}
    if (!draft.ingredientId) errors.ingredientId = 'Выбери ингредиент'
    if (!draft.mode) errors.mode = 'Выбери режим ограничения'
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0 || !draft.mode) return

    setPending(true)
    setMessage(null)
    setActionError(null)

    try {
      await saveIngredientRestriction(Number(draft.ingredientId), {
        mode: draft.mode,
        note: draft.note,
      })
      const page = await loadIngredientRestrictions()
      setState({ status: 'success', page })
      setDraft(emptyDraft)
      setEditingIngredientId(null)
      setMessage(
        editingIngredientId === null
          ? 'Ограничение добавлено'
          : 'Ограничение обновлено',
      )
    } catch (error: unknown) {
      if (error instanceof IngredientRestrictionRequestError) {
        setFieldErrors(error.fieldErrors)
        setActionError(error.message)
      } else {
        setActionError('Не удалось сохранить ограничение')
      }
    } finally {
      setPending(false)
    }
  }

  const editRule = (rule: IngredientRestrictionRule) => {
    setConfirmingDeleteId(null)
    setEditingIngredientId(rule.ingredientId)
    setDraft({
      ingredientId: String(rule.ingredientId),
      mode: rule.mode,
      note: rule.note ?? '',
    })
    setFieldErrors({})
    setMessage(null)
    setActionError(null)
  }

  const cancelEditing = () => {
    setEditingIngredientId(null)
    setDraft(emptyDraft)
    setFieldErrors({})
    setMessage(null)
    setActionError(null)
  }

  const requestDelete = (rule: IngredientRestrictionRule) => {
    setConfirmingDeleteId(rule.ingredientId)
    setMessage(null)
    setActionError(null)
  }

  const deleteRule = async (rule: IngredientRestrictionRule) => {
    setDeletingIngredientId(rule.ingredientId)
    setMessage(null)
    setActionError(null)

    try {
      await deleteIngredientRestriction(rule.ingredientId)
      const page = await loadIngredientRestrictions()
      setState({ status: 'success', page })
      setConfirmingDeleteId(null)
      if (editingIngredientId === rule.ingredientId) {
        setEditingIngredientId(null)
        setDraft(emptyDraft)
        setFieldErrors({})
      }
      setMessage('Ограничение удалено')
    } catch {
      setActionError('Не удалось удалить ограничение')
    } finally {
      setDeletingIngredientId(null)
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
            <ShieldAlert aria-hidden="true" className="size-4" />
            Личные правила
          </p>
          <h1 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
            Ограничения по ингредиентам
          </h1>
          <p className="mt-4 text-lg leading-8 text-muted-foreground">
            Настрой предупреждения или исключение рецептов с отдельными продуктами.
          </p>
        </div>
        {state.status === 'success' && (
          <Badge variant="secondary">Всего: {state.page.total}</Badge>
        )}
      </header>

      <p className="mt-7 rounded-2xl border border-amber-500/25 bg-amber-500/10 p-4 text-sm leading-6">
        Приложение выполняет заданные правила, но не определяет медицински
        безопасную частоту употребления продуктов.
      </p>

      {state.status === 'success' && (
        <RestrictionForm
          draft={draft}
          editing={editingIngredientId !== null}
          fieldErrors={fieldErrors}
          onCancel={cancelEditing}
          onChange={setDraft}
          onSubmit={saveRule}
          page={state.page}
          pending={pending}
        />
      )}

      {message && (
        <p className="mt-4 rounded-xl border border-emerald-600/25 bg-emerald-600/10 p-4 text-sm" role="status">
          {message}
        </p>
      )}
      {actionError && (
        <p className="mt-4 rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive" role="alert">
          {actionError}
        </p>
      )}

      <section aria-live="polite" className="mt-8">
        {state.status === 'loading' && <RestrictionsLoading />}
        {state.status === 'error' && (
          <RestrictionsError message={state.message} onRetry={retry} />
        )}
        {state.status === 'success' && state.page.rules.length === 0 && (
          <RestrictionsEmpty />
        )}
        {state.status === 'success' && state.page.rules.length > 0 && (
          <div className="grid gap-4 md:grid-cols-2">
            {state.page.rules.map((rule) => (
              <RestrictionCard
                confirmingDelete={confirmingDeleteId === rule.ingredientId}
                deleting={deletingIngredientId === rule.ingredientId}
                key={rule.ingredientId}
                onCancelDelete={() => setConfirmingDeleteId(null)}
                onConfirmDelete={deleteRule}
                onEdit={editRule}
                onRequestDelete={requestDelete}
                rule={rule}
              />
            ))}
          </div>
        )}
      </section>
    </main>
  )
}

function RestrictionForm({
  draft,
  editing,
  fieldErrors,
  onCancel,
  onChange,
  onSubmit,
  page,
  pending,
}: {
  draft: RestrictionDraft
  editing: boolean
  fieldErrors: Record<string, string>
  onCancel: () => void
  onChange: (draft: RestrictionDraft) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  page: IngredientRestrictionPage
  pending: boolean
}) {
  const restrictedIds = new Set(page.rules.map((rule) => rule.ingredientId))
  const ingredients = page.ingredients.filter(
    (item) => !restrictedIds.has(item.id) || (editing && String(item.id) === draft.ingredientId),
  )
  const fieldClass =
    'h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60'

  return (
    <Card className="mt-8">
      <CardHeader>
        <h2 className="font-heading text-2xl font-semibold">
          {editing ? 'Редактировать правило' : 'Добавить правило'}
        </h2>
      </CardHeader>
      <CardContent>
        <form className="grid gap-5" onSubmit={onSubmit}>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="grid gap-2 text-sm font-medium">
              Ингредиент
              <select
                aria-invalid={Boolean(fieldErrors.ingredientId)}
                className={fieldClass}
                disabled={pending || editing}
                onChange={(event) =>
                  onChange({ ...draft, ingredientId: event.target.value })
                }
                value={draft.ingredientId}
              >
                <option value="">Выбери ингредиент</option>
                {ingredients.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
              {fieldErrors.ingredientId && (
                <span className="text-sm text-destructive" role="alert">
                  {fieldErrors.ingredientId}
                </span>
              )}
            </label>

            <label className="grid gap-2 text-sm font-medium">
              Режим
              <select
                aria-invalid={Boolean(fieldErrors.mode)}
                className={fieldClass}
                disabled={pending}
                onChange={(event) =>
                  onChange({
                    ...draft,
                    mode: event.target.value as RestrictionDraft['mode'],
                  })
                }
                value={draft.mode}
              >
                <option value="">Выбери режим</option>
                {page.modes.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.name}
                  </option>
                ))}
              </select>
              {fieldErrors.mode && (
                <span className="text-sm text-destructive" role="alert">
                  {fieldErrors.mode}
                </span>
              )}
            </label>
          </div>

          <label className="grid gap-2 text-sm font-medium">
            Личная заметка
            <textarea
              aria-invalid={Boolean(fieldErrors.note)}
              className="min-h-24 w-full rounded-lg border bg-background px-3 py-2 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20"
              disabled={pending}
              maxLength={1000}
              onChange={(event) => onChange({ ...draft, note: event.target.value })}
              placeholder="Например: можно редко или только небольшую порцию"
              value={draft.note}
            />
            {fieldErrors.note && (
              <span className="text-sm text-destructive" role="alert">
                {fieldErrors.note}
              </span>
            )}
          </label>

          <div className="flex flex-wrap gap-3">
            <Button disabled={pending || (!editing && ingredients.length === 0)} type="submit">
              {pending
                ? 'Сохраняем...'
                : editing
                  ? 'Сохранить изменения'
                  : 'Добавить правило'}
            </Button>
            {editing && (
              <Button disabled={pending} onClick={onCancel} type="button" variant="outline">
                Отменить
              </Button>
            )}
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

function RestrictionCard({
  confirmingDelete,
  deleting,
  onCancelDelete,
  onConfirmDelete,
  onEdit,
  onRequestDelete,
  rule,
}: {
  confirmingDelete: boolean
  deleting: boolean
  onCancelDelete: () => void
  onConfirmDelete: (rule: IngredientRestrictionRule) => void
  onEdit: (rule: IngredientRestrictionRule) => void
  onRequestDelete: (rule: IngredientRestrictionRule) => void
  rule: IngredientRestrictionRule
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between gap-4">
        <h2 className="font-heading text-xl font-semibold">
          {rule.ingredientName}
        </h2>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <Badge variant={rule.mode === 'EXCLUDE' ? 'destructive' : 'secondary'}>
            {rule.modeName}
          </Badge>
          <Button
            aria-label={`Редактировать ограничение: ${rule.ingredientName}`}
            disabled={deleting}
            onClick={() => onEdit(rule)}
            size="sm"
            type="button"
            variant="outline"
          >
            <Pencil aria-hidden="true" data-icon="inline-start" />
            Редактировать
          </Button>
          <Button
            aria-label={`Удалить ограничение: ${rule.ingredientName}`}
            disabled={deleting}
            onClick={() => onRequestDelete(rule)}
            size="sm"
            type="button"
            variant="destructive"
          >
            <Trash2 aria-hidden="true" data-icon="inline-start" />
            Удалить
          </Button>
        </div>
      </CardHeader>
      {rule.note && (
        <CardContent>
          <p className="leading-7 text-muted-foreground">{rule.note}</p>
        </CardContent>
      )}
      {confirmingDelete && (
        <CardContent>
          <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4">
            <p className="font-medium">
              Удалить ограничение для «{rule.ingredientName}»?
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              <Button
                aria-label={`Подтвердить удаление: ${rule.ingredientName}`}
                disabled={deleting}
                onClick={() => onConfirmDelete(rule)}
                type="button"
                variant="destructive"
              >
                {deleting ? 'Удаляем...' : 'Да, удалить'}
              </Button>
              <Button
                aria-label={`Отменить удаление: ${rule.ingredientName}`}
                disabled={deleting}
                onClick={onCancelDelete}
                type="button"
                variant="outline"
              >
                Отменить
              </Button>
            </div>
          </div>
        </CardContent>
      )}
    </Card>
  )
}

function RestrictionsLoading() {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <span className="sr-only">Загружаем ограничения по ингредиентам</span>
      {[0, 1].map((item) => (
        <div className="h-36 animate-pulse rounded-2xl border bg-card" key={item} />
      ))}
    </div>
  )
}

function RestrictionsEmpty() {
  return (
    <div className="rounded-3xl border bg-card px-6 py-14 text-center shadow-sm">
      <span className="mx-auto grid size-16 place-items-center rounded-full bg-secondary text-primary">
        <ShieldAlert aria-hidden="true" className="size-7" />
      </span>
      <h2 className="mt-5 font-heading text-2xl font-semibold">
        Ограничения пока не настроены
      </h2>
      <p className="mx-auto mt-3 max-w-lg text-muted-foreground">
        Здесь появятся продукты, для которых нужно показывать предупреждение или
        исключать рецепты.
      </p>
    </div>
  )
}

function RestrictionsError({
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
        Ограничения пока не загрузились
      </h2>
      <p className="mt-3 text-muted-foreground" role="alert">
        {message}
      </p>
      <Button className="mt-6" onClick={onRetry} variant="outline">
        <RefreshCw aria-hidden="true" data-icon="inline-start" />
        Повторить
      </Button>
    </div>
  )
}
