import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { KitchenRecipesPage } from './KitchenRecipesPage'
import type { IKitchenApi } from '../model/IKitchenApi'
import type { Recipe } from '../model/types'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const RECIPES: Recipe[] = [
  { id: 'r1', name: 'Pâtes bolognaise', category: 'PLAT', minutes: 35, referencePortions: 4, favorite: false, ingredients: [], steps: [] },
  { id: 'r2', name: 'Curry de légumes', category: 'VEGETARIAN', minutes: 30, referencePortions: 4, favorite: true, ingredients: [], steps: [] },
]

function fakeApi(overrides: Partial<IKitchenApi> = {}): IKitchenApi {
  return {
    listRecipes: vi.fn().mockResolvedValue(RECIPES),
    getRecipe: vi.fn(),
    createRecipe: vi.fn(),
    updateRecipe: vi.fn(),
    deleteRecipe: vi.fn().mockResolvedValue(undefined),
    toggleFavorite: vi.fn().mockResolvedValue(RECIPES[0]),
    listMenuEntries: vi.fn(),
    addMenuEntry: vi.fn(),
    updateMenuEntryPortions: vi.fn(),
    removeMenuEntry: vi.fn(),
    getShoppingList: vi.fn(),
    ...overrides,
  }
}

function setup(api: IKitchenApi = fakeApi()) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/s/space-1/kitchen/recipes']}>
        <Routes>
          <Route path="/s/:spaceId/kitchen/recipes" element={<KitchenRecipesPage api={api} />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
  return { api }
}

describe('KitchenRecipesPage', () => {
  it('lists every recipe for the space', async () => {
    setup()

    expect(await screen.findByText('Pâtes bolognaise')).toBeDefined()
    expect(screen.getByText('Curry de légumes')).toBeDefined()
  })

  it('filters by name as you type', async () => {
    setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'curry' } })

    expect(screen.queryByText('Pâtes bolognaise')).toBeNull()
    expect(screen.getByText('Curry de légumes')).toBeDefined()
  })

  it('deletes a recipe', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getAllByText('delete')[0])

    await waitFor(() => expect(api.deleteRecipe).toHaveBeenCalledWith('space-1', 'r1'))
  })

  it('toggles favorite', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getAllByLabelText('favorite')[0])

    await waitFor(() => expect(api.toggleFavorite).toHaveBeenCalledWith('space-1', 'r1'))
  })
})
