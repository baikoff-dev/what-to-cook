import { type FormEvent, type ReactNode, useState } from 'react'
import { ArrowLeft, CalendarCheck, LoaderCircle } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  addCookingRecord,
  CookingHistoryApiError,
} from '@/features/history/api/cooking-history'

const fieldClass =
  'mt-2 h-11 w-full rounded-lg border bg-background px-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20'

export function CookingRecordPage() {
  const { recipeId } = useParams()
  const id = Number(recipeId)
  const navigate = useNavigate()
  const [cookedOn, setCookedOn] = useState(today())
  const [rating, setRating] = useState('')
  const [note, setNote] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!Number.isInteger(id) || id < 1 || pending) {
      return
    }

    setPending(true)
    setError(null)
    setFieldErrors({})

    try {
      await addCookingRecord(id, {
        cookedOn,
        rating: rating ? Number(rating) : null,
        note,
      })
      navigate('/recipes/history')
    } catch (requestError: unknown) {
      if (requestError instanceof CookingHistoryApiError) {
        setFieldErrors(requestError.fieldErrors)
      }
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Не удалось добавить приготовление',
      )
    } finally {
      setPending(false)
    }
  }

  if (!Number.isInteger(id) || id < 1) {
    return (
      <main className="mx-auto w-full max-w-3xl px-5 py-10 sm:px-8 sm:py-14">
        <h1 className="font-heading text-3xl font-semibold">Рецепт не найден</h1>
        <Button asChild className="mt-6">
          <Link to="/">Вернуться в каталог</Link>
        </Button>
      </main>
    )
  }

  return (
    <main className="mx-auto w-full max-w-3xl px-5 py-10 sm:px-8 sm:py-14">
      <Button asChild variant="ghost">
        <Link to={`/recipes/${id}`}>
          <ArrowLeft aria-hidden="true" data-icon="inline-start" />
          Вернуться к рецепту
        </Link>
      </Button>

      <header className="mt-7 max-w-2xl">
        <p className="mb-3 flex items-center gap-2 text-sm font-semibold tracking-[0.16em] text-primary uppercase">
          <CalendarCheck aria-hidden="true" className="size-4" />
          Личный результат
        </p>
        <h1 className="font-heading text-4xl font-semibold tracking-tight sm:text-5xl">
          Отметить приготовление
        </h1>
        <p className="mt-4 text-lg leading-8 text-muted-foreground">
          Сохрани дату, оценку и короткую заметку, чтобы в следующий раз блюдо
          получилось ещё лучше.
        </p>
      </header>

      <Card className="mt-8">
        <CardHeader>
          <CardTitle>Новая запись</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-6" onSubmit={submit}>
            <FormField error={fieldErrors.cookedOn} label="Дата приготовления">
              <input
                className={fieldClass}
                id="cooked-on"
                max={today()}
                onChange={(event) => setCookedOn(event.target.value)}
                required
                type="date"
                value={cookedOn}
              />
            </FormField>

            <FormField error={fieldErrors.rating} label="Оценка">
              <select
                className={fieldClass}
                id="rating"
                onChange={(event) => setRating(event.target.value)}
                value={rating}
              >
                <option value="">Без оценки</option>
                {[1, 2, 3, 4, 5].map((value) => (
                  <option key={value} value={value}>{value}</option>
                ))}
              </select>
            </FormField>

            <FormField error={fieldErrors.note} label="Заметка">
              <textarea
                className="mt-2 min-h-32 w-full rounded-lg border bg-background p-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20"
                id="note"
                maxLength={2000}
                onChange={(event) => setNote(event.target.value)}
                placeholder="Что изменить или повторить в следующий раз?"
                value={note}
              />
            </FormField>

            {error && (
              <p className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive" role="alert">
                {error}
              </p>
            )}

            <div className="flex flex-wrap gap-3">
              <Button disabled={pending} type="submit">
                {pending && <LoaderCircle aria-hidden="true" className="animate-spin" />}
                {pending ? 'Сохраняем...' : 'Добавить в историю'}
              </Button>
              <Button asChild variant="outline">
                <Link to={`/recipes/${id}`}>Отмена</Link>
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </main>
  )
}

function FormField({
  children,
  error,
  label,
}: {
  children: ReactNode
  error?: string
  label: string
}) {
  const id = label === 'Дата приготовления' ? 'cooked-on' : label === 'Оценка' ? 'rating' : 'note'
  return (
    <div>
      <label className="text-sm font-semibold" htmlFor={id}>{label}</label>
      {children}
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
    </div>
  )
}

function today() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}
