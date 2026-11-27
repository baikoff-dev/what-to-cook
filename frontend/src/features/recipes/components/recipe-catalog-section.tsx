import { type FormEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  AlertTriangle,
  Archive,
  Heart,
  History,
  PackageOpen,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  ShoppingBasket,
  ShieldAlert,
  Sparkles,
} from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  emptyRecipeCatalogFilters,
  loadRecipeCatalog,
  loadRecipeCatalogFilterOptions,
  type RecipeCatalogFilterOptions,
  type RecipeCatalogFilters,
  type RecipeCatalogItem,
} from '@/features/recipes/api/recipe-catalog'
import { RecipeCard } from '@/features/recipes/components/recipe-card'

type CatalogState =
  | { status: 'loading' }
  | { status: 'success'; items: RecipeCatalogItem[]; total: number }
  | { status: 'error' }

type FilterOptionsState =
  | { status: 'loading' }
  | { status: 'success'; options: RecipeCatalogFilterOptions }
  | { status: 'error' }

const shortcuts = [
  { icon: Sparkles, label: 'Помощник выбора', path: '/assistant' },
  { icon: ShoppingBasket, label: 'Подбор по продуктам', path: '/recipes/match' },
  { icon: PackageOpen, label: 'Кладовая', path: '/preferences/pantry' },
  { icon: Heart, label: 'Избранное', path: '/recipes/favorites' },
  { icon: History, label: 'История', path: '/recipes/history' },
  { icon: ShieldAlert, label: 'Ограничения', path: '/preferences/restrictions' },
  { icon: Archive, label: 'Архив', path: '/recipes/archive' },
]

export function RecipeCatalogSection() {
  const [requestNumber, setRequestNumber] = useState(0)
  const [draftFilters, setDraftFilters] = useState<RecipeCatalogFilters>({
    ...emptyRecipeCatalogFilters,
  })
  const [appliedFilters, setAppliedFilters] = useState<RecipeCatalogFilters>({
    ...emptyRecipeCatalogFilters,
  })
  const [catalog, setCatalog] = useState<CatalogState>({ status: 'loading' })
  const [filterOptions, setFilterOptions] = useState<FilterOptionsState>({
    status: 'loading',
  })

  useEffect(() => {
    let active = true

    loadRecipeCatalogFilterOptions()
      .then((options) => {
        if (active) {
          setFilterOptions({ status: 'success', options })
        }
      })
      .catch(() => {
        if (active) {
          setFilterOptions({ status: 'error' })
        }
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    let active = true

    loadRecipeCatalog(appliedFilters)
      .then((response) => {
        if (active) {
          setCatalog({ status: 'success', ...response })
        }
      })
      .catch(() => {
        if (active) {
          setCatalog({ status: 'error' })
        }
      })

    return () => {
      active = false
    }
  }, [appliedFilters, requestNumber])

  const applyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setCatalog({ status: 'loading' })
    setAppliedFilters({ ...draftFilters })
  }

  const resetFilters = () => {
    const emptyFilters = { ...emptyRecipeCatalogFilters }
    setDraftFilters(emptyFilters)
    setCatalog({ status: 'loading' })
    setAppliedFilters(emptyFilters)
  }

  const retry = () => {
    setCatalog({ status: 'loading' })
    setRequestNumber((value) => value + 1)
  }

  const updateFilter = (name: keyof RecipeCatalogFilters, value: string) => {
    setDraftFilters((current) => ({ ...current, [name]: value }))
  }

  const filtersApplied = Object.values(appliedFilters).some(Boolean)

  return (
    <section className="border-y border-border/70 bg-card/55" id="recipes">
      <div className="mx-auto max-w-6xl px-5 py-14 sm:px-8 sm:py-20">
        <div className="mb-8 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-2xl">
            <p className="mb-3 text-sm font-semibold tracking-[0.16em] text-primary uppercase">
              Каталог
            </p>
            <h2 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
              Рецепты из твоей коллекции
            </h2>
            <p className="mt-3 text-muted-foreground">
              Найди блюдо по продуктам, времени и способу приготовления.
            </p>
          </div>
          <Button asChild>
            <Link to="/recipes/new">
              <Plus aria-hidden="true" data-icon="inline-start" />
              Добавить рецепт
            </Link>
          </Button>
        </div>

        <nav aria-label="Разделы приложения" className="mb-6 flex flex-wrap gap-2">
          {shortcuts.map(({ icon: Icon, label, path }) => (
            <Button asChild key={path} size="sm" variant="outline">
              <Link to={path}>
                <Icon aria-hidden="true" data-icon="inline-start" />
                {label}
              </Link>
            </Button>
          ))}
        </nav>

        <CatalogFilters
          filters={draftFilters}
          filterOptions={filterOptions}
          filtersApplied={filtersApplied}
          onChange={updateFilter}
          onReset={resetFilters}
          onSubmit={applyFilters}
        />

        <div className="mt-8 mb-5 flex items-center justify-between gap-4">
          <p className="text-sm text-muted-foreground">
            {filtersApplied ? 'Результат поиска' : 'Весь каталог'}
          </p>
          {catalog.status === 'success' && (
            <Badge variant="secondary">Всего: {catalog.total}</Badge>
          )}
        </div>

        {catalog.status === 'loading' && <CatalogLoading />}
        {catalog.status === 'error' && <CatalogError onRetry={retry} />}
        {catalog.status === 'success' && catalog.items.length === 0 && (
          <CatalogEmpty filtersApplied={filtersApplied} onReset={resetFilters} />
        )}
        {catalog.status === 'success' && catalog.items.length > 0 && (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {catalog.items.map((recipe) => (
              <RecipeCard key={recipe.id} recipe={recipe} />
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

function CatalogFilters({
  filters,
  filterOptions,
  filtersApplied,
  onChange,
  onReset,
  onSubmit,
}: {
  filters: RecipeCatalogFilters
  filterOptions: FilterOptionsState
  filtersApplied: boolean
  onChange: (name: keyof RecipeCatalogFilters, value: string) => void
  onReset: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
  const options = filterOptions.status === 'success' ? filterOptions.options : null
  const fieldClass =
    'h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60'

  return (
    <form
      aria-label="Поиск и фильтры рецептов"
      className="rounded-2xl border bg-background/90 p-4 shadow-sm sm:p-5"
      onSubmit={onSubmit}
    >
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <label className="grid gap-2 text-sm font-medium lg:col-span-2">
          Название или ингредиент
          <input
            className={fieldClass}
            onChange={(event) => onChange('query', event.target.value)}
            placeholder="Например, курица или гречка"
            type="search"
            value={filters.query}
          />
        </label>
        <label className="grid gap-2 text-sm font-medium">
          Не дольше, минут
          <input
            className={fieldClass}
            max="2880"
            min="1"
            onChange={(event) => onChange('maxMinutes', event.target.value)}
            placeholder="Любое время"
            type="number"
            value={filters.maxMinutes}
          />
        </label>
        <FilterSelect
          label="Сложность"
          name="difficulty"
          onChange={onChange}
          options={options?.difficulties ?? []}
          placeholder="Любая"
          value={filters.difficulty}
        />
        <FilterSelect
          label="Оборудование"
          name="equipmentId"
          onChange={onChange}
          options={(options?.equipment ?? []).map((item) => ({
            value: String(item.id),
            name: item.name,
          }))}
          placeholder="Любое"
          value={filters.equipmentId}
        />
        <FilterSelect
          label="Способ приготовления"
          name="cookingMethodId"
          onChange={onChange}
          options={(options?.cookingMethods ?? []).map((item) => ({
            value: String(item.id),
            name: item.name,
          }))}
          placeholder="Любой"
          value={filters.cookingMethodId}
        />
        <FilterSelect
          label="Кухня"
          name="cuisineId"
          onChange={onChange}
          options={(options?.cuisines ?? []).map((item) => ({
            value: String(item.id),
            name: item.name,
          }))}
          placeholder="Любая"
          value={filters.cuisineId}
        />
        <FilterSelect
          label="Происхождение"
          name="origin"
          onChange={onChange}
          options={options?.origins ?? []}
          placeholder="Любое"
          value={filters.origin}
        />
      </div>

      {filterOptions.status === 'error' && (
        <p className="mt-4 text-sm text-destructive">
          Не удалось загрузить дополнительные варианты фильтров.
        </p>
      )}

      <div className="mt-5 flex flex-wrap gap-3">
        <Button type="submit">
          <Search aria-hidden="true" data-icon="inline-start" />
          Найти
        </Button>
        {(filtersApplied || Object.values(filters).some(Boolean)) && (
          <Button onClick={onReset} type="button" variant="outline">
            <RotateCcw aria-hidden="true" data-icon="inline-start" />
            Сбросить
          </Button>
        )}
      </div>
    </form>
  )
}

function FilterSelect({
  label,
  name,
  onChange,
  options,
  placeholder,
  value,
}: {
  label: string
  name: keyof RecipeCatalogFilters
  onChange: (name: keyof RecipeCatalogFilters, value: string) => void
  options: Array<{ value: string; name: string }>
  placeholder: string
  value: string
}) {
  return (
    <label className="grid gap-2 text-sm font-medium">
      {label}
      <select
        className="h-10 w-full rounded-lg border bg-background px-3 text-sm outline-none transition-shadow focus:border-ring focus:ring-3 focus:ring-ring/20"
        onChange={(event) => onChange(name, event.target.value)}
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

function CatalogLoading() {
  return (
    <div aria-live="polite" className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <span className="sr-only">Загружаем рецепты</span>
      {[0, 1, 2].map((item) => (
        <div
          className="h-96 animate-pulse rounded-2xl border bg-background/80"
          key={item}
        />
      ))}
    </div>
  )
}

function CatalogEmpty({
  filtersApplied,
  onReset,
}: {
  filtersApplied: boolean
  onReset: () => void
}) {
  return (
    <div className="rounded-2xl border bg-background p-8 text-center">
      <Search aria-hidden="true" className="mx-auto size-9 text-primary" />
      <h3 className="mt-4 font-heading text-xl font-semibold">Рецепты не найдены</h3>
      <p className="mt-2 text-muted-foreground">
        {filtersApplied
          ? 'Попробуй изменить условия поиска или сбросить фильтры.'
          : 'В каталоге пока нет рецептов.'}
      </p>
      {filtersApplied && (
        <Button className="mt-5" onClick={onReset} variant="outline">
          Сбросить фильтры
        </Button>
      )}
    </div>
  )
}

function CatalogError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="rounded-2xl border border-destructive/30 bg-background p-8 text-center">
      <AlertTriangle aria-hidden="true" className="mx-auto size-9 text-destructive" />
      <h3 className="mt-4 font-heading text-xl font-semibold">
        Каталог пока не загрузился
      </h3>
      <p className="mt-2 text-muted-foreground">
        Проверь, что Spring Boot запущен на порту 8080, и попробуй ещё раз.
      </p>
      <Button className="mt-5" onClick={onRetry} variant="outline">
        <RefreshCw aria-hidden="true" data-icon="inline-start" />
        Повторить
      </Button>
    </div>
  )
}
