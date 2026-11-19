import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from './App'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  window.history.replaceState(null, '', '/')
})

describe('технический экран frontend', () => {
  it('показывает готовую основу и направление следующего шага', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { name: /выбирай блюдо/i }),
    ).toBeInTheDocument()
    expect(screen.getByText('React + TypeScript')).toBeInTheDocument()
    expect(screen.getByText(/React получает реальные данные Spring Boot/i)).toBeInTheDocument()
  })
})

describe('каталог рецептов', () => {
  it('загружает рецепты из backend и показывает их пользователю', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input) => {
      if (String(input) === '/api/recipes/filter-options') {
        return response({
          difficulties: [],
          equipment: [],
          cookingMethods: [],
          cuisines: [],
          origins: [],
        })
      }

      return response({
        items: [
          {
            id: 1,
            name: 'Гречка с тушёной курицей',
            englishName: 'Buckwheat with stewed chicken',
            description: 'Сытное блюдо из гречки, куриного филе и овощей.',
            coverUrl: '/images/recipes/buckwheat-with-chicken.jpg',
            totalMinutes: 50,
            servings: 2,
            difficulty: 'Легко',
            status: 'Не проверен',
            cuisine: 'Восточнославянская',
            origin: 'Личный',
            favorite: false,
            restrictionWarnings: [],
          },
        ],
        total: 1,
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: 'Гречка с тушёной курицей',
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('50 мин')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes')
  })

  it('применяет выбранные фильтры и показывает найденные рецепты', async () => {
    const initialCatalog = {
      items: [
        recipe({ id: 1, name: 'Гречка с тушёной курицей' }),
        recipe({ id: 2, name: 'Белорусские драники' }),
      ],
      total: 2,
    }
    const filteredCatalog = {
      items: [recipe({ id: 1, name: 'Гречка с тушёной курицей' })],
      total: 1,
    }
    const filterOptions = {
      difficulties: [{ value: 'EASY', name: 'Легко' }],
      equipment: [{ id: 1, name: 'Электрическая плита' }],
      cookingMethods: [{ id: 1, name: 'Тушение' }],
      cuisines: [{ id: 1, name: 'Восточнославянская' }],
      origins: [{ value: 'CLASSIC', name: 'Классический' }],
    }
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input) => {
      const url = String(input)

      if (url === '/api/recipes/filter-options') {
        return response(filterOptions)
      }
      if (url.startsWith('/api/recipes?')) {
        return response(filteredCatalog)
      }
      return response(initialCatalog)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)

    expect(
      await screen.findByRole('heading', { name: 'Белорусские драники' }),
    ).toBeInTheDocument()

    fireEvent.change(
      screen.getByRole('searchbox', { name: 'Название или ингредиент' }),
      { target: { value: 'гречка' } },
    )
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Не дольше, минут' }), {
      target: { value: '50' },
    })
    fireEvent.change(screen.getByRole('combobox', { name: 'Сложность' }), {
      target: { value: 'EASY' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Найти' }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/recipes?query=%D0%B3%D1%80%D0%B5%D1%87%D0%BA%D0%B0&maxMinutes=50&difficulty=EASY',
      )
    })
    expect(
      await screen.findByRole('heading', { name: 'Гречка с тушёной курицей' }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('heading', { name: 'Белорусские драники' }),
    ).not.toBeInTheDocument()
    expect(screen.getByText('Всего: 1')).toBeInTheDocument()
  })
})

describe('страница рецепта', () => {
  it('открывает рецепт из каталога и пересчитывает ингредиенты на выбранное число порций', async () => {
    const catalogRecipe = recipe({
      id: 1,
      name: 'Гречка с тушёной курицей',
    })
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input) => {
      const url = String(input)

      if (url === '/api/recipes/filter-options') {
        return response({
          difficulties: [],
          equipment: [],
          cookingMethods: [],
          cuisines: [],
          origins: [],
        })
      }
      if (url === '/api/recipes/1') {
        return response(recipeDetails(2, '300'))
      }
      if (url === '/api/recipes/1?servings=4') {
        return response(recipeDetails(4, '600'))
      }
      return response({ items: [catalogRecipe], total: 1 })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)

    fireEvent.click(
      await screen.findByRole('link', { name: 'Гречка с тушёной курицей' }),
    )

    expect(
      await screen.findByRole('heading', { name: 'Ингредиенты' }),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes/1')
    expect(screen.getByText('300 г')).toBeInTheDocument()

    fireEvent.change(screen.getByRole('spinbutton', { name: 'Порций' }), {
      target: { value: '4' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Пересчитать' }))

    expect(await screen.findByText('600 г')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes/1?servings=4')
  })

  it('добавляет рецепт в избранное и удаляет его без перезагрузки страницы', async () => {
    let favorite = false
    const catalogRecipe = recipe({
      id: 1,
      name: 'Гречка с тушёной курицей',
    })
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input)

      if (url === '/api/recipes/filter-options') {
        return response({
          difficulties: [],
          equipment: [],
          cookingMethods: [],
          cuisines: [],
          origins: [],
        })
      }
      if (url === '/api/recipes/1/favorite' && init?.method === 'PUT') {
        favorite = true
        return response({ favorite })
      }
      if (url === '/api/recipes/1/favorite' && init?.method === 'DELETE') {
        favorite = false
        return response({ favorite })
      }
      if (url === '/api/recipes/1') {
        return response(recipeDetails(2, '300', favorite))
      }
      return response({ items: [catalogRecipe], total: 1 })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    fireEvent.click(
      await screen.findByRole('link', { name: 'Гречка с тушёной курицей' }),
    )

    const addButton = await screen.findByRole('button', {
      name: 'Добавить в избранное',
    })
    fireEvent.click(addButton)

    expect(
      await screen.findByRole('button', { name: 'В избранном' }),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes/1/favorite', {
      method: 'PUT',
    })

    fireEvent.click(screen.getByRole('button', { name: 'В избранном' }))

    expect(
      await screen.findByRole('button', { name: 'Добавить в избранное' }),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes/1/favorite', {
      method: 'DELETE',
    })
  })
})

describe('страница избранного', () => {
  it('открывает избранные рецепты и удаляет карточку без перехода в старый интерфейс', async () => {
    let favorite = true
    const favoriteRecipe = {
      ...recipe({ id: 1, name: 'Гречка с тушёной курицей' }),
      favorite: true,
    }
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input)

      if (url === '/api/recipes/filter-options') {
        return response({
          difficulties: [],
          equipment: [],
          cookingMethods: [],
          cuisines: [],
          origins: [],
        })
      }
      if (url === '/api/recipes/favorites') {
        return response({
          items: favorite ? [favoriteRecipe] : [],
          total: favorite ? 1 : 0,
        })
      }
      if (url === '/api/recipes/1/favorite' && init?.method === 'DELETE') {
        favorite = false
        return response({ favorite })
      }
      return response({ items: [favoriteRecipe], total: 1 })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    fireEvent.click(await screen.findByRole('link', { name: 'Избранное' }))

    expect(
      await screen.findByRole('heading', { name: 'Избранные рецепты' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Гречка с тушёной курицей' }),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes/favorites')

    fireEvent.click(
      screen.getByRole('button', { name: 'Удалить из избранного' }),
    )

    expect(
      await screen.findByText('В избранном пока нет рецептов'),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes/1/favorite', {
      method: 'DELETE',
    })
  })
})

function recipe(overrides: { id: number; name: string }) {
  return {
    id: overrides.id,
    name: overrides.name,
    englishName: 'Recipe',
    description: 'Описание рецепта.',
    coverUrl: null,
    totalMinutes: 50,
    servings: 2,
    difficulty: 'Легко',
    status: 'Не проверен',
    cuisine: 'Восточнославянская',
    origin: 'Классический',
    favorite: false,
    restrictionWarnings: [],
  }
}

function recipeDetails(
  servings: number,
  chickenAmount: string,
  favorite = false,
) {
  return {
    id: 1,
    name: 'Гречка с тушёной курицей',
    englishName: 'Buckwheat with stewed chicken',
    description: 'Сытное блюдо из гречки, куриного филе и овощей.',
    coverUrl: null,
    baseServings: 2,
    servings,
    preparationMinutes: 15,
    cookingMinutes: 35,
    totalMinutes: 50,
    difficulty: 'Легко',
    status: 'Не проверен',
    cuisine: 'Восточнославянская',
    origin: 'Личный',
    sourceName: 'Демонстрационный рецепт проекта',
    sourceUrl: null,
    videoUrl: null,
    personalNotes: 'Проверить количество воды и соли.',
    ingredients: [
      {
        name: 'Куриное филе',
        amount: chickenAmount,
        unit: 'г',
        optional: false,
        substitutions: [],
      },
    ],
    steps: [{ number: 1, instruction: 'Промыть гречку.', imageUrl: null }],
    equipment: ['Электрическая плита'],
    cookingMethods: ['Тушение'],
    favorite,
    excludedByRestriction: false,
    restrictionWarnings: [],
  }
}

function response(body: unknown) {
  return {
    ok: true,
    json: async () => body,
  } as Response
}
