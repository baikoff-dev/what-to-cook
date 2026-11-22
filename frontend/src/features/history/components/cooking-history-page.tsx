import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  ArrowLeft,
  CalendarDays,
  History,
  RefreshCw,
  Star,
  Trash2,
} from 'lucide-react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  deleteCookingRecord,
  loadCookingHistory,
  type CookingHistoryItem,
} from '@/features/history/api/cooking-history'

type HistoryState =
  | { status: 'loading' }
  | { status: 'success'; items: CookingHistoryItem[]; total: number }
  | { status: 'error'; message: string }

export function CookingHistoryPage() {
  const [state, setState] = useState<HistoryState>({ status: 'loading' })
  const [requestNumber, setRequestNumber] = useState(0)
  const [pendingRecordId, setPendingRecordId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    loadCookingHistory()
      .then((history) => {
        if (active) setState({ status: 'success', ...history })
      })
      .catch((error: unknown) => {
        if (active) {
          setState({
            status: 'error',
            message:
              error instanceof Error
                ? error.message
                : 'Не удалось загрузить историю приготовления',
          })
        }
      })

    return () => {
      active = false
    }
  }, [requestNumber])

  const remove = async (item: CookingHistoryItem) => {
    if (pendingRecordId !== null) return
    setPendingRecordId(item.id)
    setActionError(null)

    try {
      await deleteCookingRecord(item.id)
      setState((current) => {
        if (current.status !== 'success') return current
        const items = current.items.filter((record) => record.id !== item.id)
        return { status: 'success', items, total: items.length }
      })
    } catch (error: unknown) {
      setActionError(
        error instanceof Error ? error.message : 'Не удалось удалить запись',
      )
    } finally {
      setPendingRecordId(null)
    }
  }

  const retry = () => {
    setState({ status: 'loading' })
    setRequestNumber((value) => value + 1)
  }

  return (
    <main className="mx-auto w-full max-w-5xl px-5 py-10 sm:px-8 sm:py-14">
      <Button asChild variant="ghost">
        <Link to="/#recipes">
          <ArrowLeft aria-hidden="true" data-icon="inline-start" />
          Вернуться в каталог
        </Link>
      </Button>

      <header className="mt-7 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div className="max-w-2xl">
          <p className="mb-3 flex items-center gap-2 text-sm font-semibold tracking-[0.16em] text-primary uppercase">
            <History aria-hidden="true" className="size-4" />
            Личный опыт
          </p>
          <h1 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
            История приготовления
          </h1>
          <p className="mt-4 text-lg leading-8 text-muted-foreground">
            Все приготовленные блюда, оценки и заметки в одном месте.
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
        {state.status === 'loading' && <HistoryLoading />}
        {state.status === 'error' && (
          <HistoryError message={state.message} onRetry={retry} />
        )}
        {state.status === 'success' && state.items.length === 0 && <HistoryEmpty />}
        {state.status === 'success' && state.items.length > 0 && (
          <div className="grid gap-4">
            {state.items.map((item) => (
              <HistoryCard
                item={item}
                key={item.id}
                onDelete={remove}
                pending={pendingRecordId === item.id}
              />
            ))}
          </div>
        )}
      </section>
    </main>
  )
}

function HistoryCard({
  item,
  onDelete,
  pending,
}: {
  item: CookingHistoryItem
  onDelete: (item: CookingHistoryItem) => void
  pending: boolean
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
            <CalendarDays aria-hidden="true" className="size-4" />
            <time dateTime={item.cookedOn}>{formatDate(item.cookedOn)}</time>
            {item.archived && <Badge variant="outline">Рецепт в архиве</Badge>}
          </div>
          <CardTitle>
            {item.archived ? (
              item.recipeName
            ) : (
              <Link className="hover:text-primary" to={`/recipes/${item.recipeId}`}>
                {item.recipeName}
              </Link>
            )}
          </CardTitle>
        </div>
        <Button
          aria-label={`Удалить запись о приготовлении: ${item.recipeName}`}
          disabled={pending}
          onClick={() => onDelete(item)}
          size="icon"
          variant="ghost"
        >
          <Trash2 aria-hidden="true" />
        </Button>
      </CardHeader>
      {(item.rating !== null || item.note) && (
        <CardContent className="grid gap-3">
          {item.rating !== null && (
            <p className="flex items-center gap-2 font-medium">
              <Star aria-hidden="true" className="size-4 fill-current text-amber-500" />
              Оценка: {item.rating} из 5
            </p>
          )}
          {item.note && <p className="leading-7 text-muted-foreground">{item.note}</p>}
        </CardContent>
      )}
    </Card>
  )
}

function HistoryLoading() {
  return (
    <div className="grid gap-4">
      <span className="sr-only">Загружаем историю приготовления</span>
      {[0, 1].map((item) => (
        <div className="h-36 animate-pulse rounded-2xl border bg-card" key={item} />
      ))}
    </div>
  )
}

function HistoryEmpty() {
  return (
    <div className="rounded-3xl border bg-card px-6 py-14 text-center shadow-sm">
      <span className="mx-auto grid size-16 place-items-center rounded-full bg-secondary text-primary">
        <History aria-hidden="true" className="size-7" />
      </span>
      <h2 className="mt-5 font-heading text-2xl font-semibold">История пока пуста</h2>
      <p className="mx-auto mt-3 max-w-lg text-muted-foreground">
        Открой рецепт и нажми «Отметить приготовленным» — запись появится здесь.
      </p>
      <Button asChild className="mt-6">
        <Link to="/#recipes">Выбрать рецепт</Link>
      </Button>
    </div>
  )
}

function HistoryError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="rounded-3xl border border-destructive/30 bg-card px-6 py-14 text-center">
      <AlertTriangle aria-hidden="true" className="mx-auto size-10 text-destructive" />
      <h2 className="mt-5 font-heading text-2xl font-semibold">История пока не загрузилась</h2>
      <p className="mt-3 text-muted-foreground" role="alert">{message}</p>
      <Button className="mt-6" onClick={onRetry} variant="outline">
        <RefreshCw aria-hidden="true" data-icon="inline-start" />
        Повторить
      </Button>
    </div>
  )
}

function formatDate(date: string) {
  const [year, month, day] = date.split('-')
  return `${day}.${month}.${year}`
}
