import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { KitchenMenuPage } from './KitchenMenuPage'
import type { IKitchenApi } from '../model/IKitchenApi'
import type { MenuEntry, Recipe, ShoppingListLine } from '../model/types'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const RECIPES: Recipe[] = [
  { id: 'r1', name: 'Pâtes bolognaise', category: 'PLAT', minutes: 35, referencePortions: 4, favorite: false, ingredients: [], steps: [], lastPlannedOn: '2026-08-01' },
  { id: 'r2', name: 'Curry de légumes', category: 'VEGETARIAN', minutes: 30, referencePortions: 4, favorite: false, ingredients: [], steps: [], lastPlannedOn: null },
]

const ENTRIES: MenuEntry[] = [
  { id: 'e1', date: '2026-09-07', recipeId: 'r1', recipeName: 'Pâtes bolognaise', recipeCategory: 'PLAT', portions: 4, position: 0 },
]

const SHOPPING_LIST: ShoppingListLine[] = [{ name: 'Pâtes', quantity: 500, unit: 'GRAM' }]

function fakeApi(overrides: Partial<IKitchenApi> = {}): IKitchenApi {
  return {
    listRecipes: vi.fn().mockResolvedValue(RECIPES),
    getRecipe: vi.fn(),
    createRecipe: vi.fn(),
    updateRecipe: vi.fn(),
    deleteRecipe: vi.fn(),
    toggleFavorite: vi.fn(),
    listMenuEntries: vi.fn().mockResolvedValue(ENTRIES),
    addMenuEntry: vi.fn().mockResolvedValue(ENTRIES[0]),
    updateMenuEntryPortions: vi.fn().mockResolvedValue(undefined),
    removeMenuEntry: vi.fn().mockResolvedValue(undefined),
    getShoppingList: vi.fn().mockResolvedValue(SHOPPING_LIST),
    ...overrides,
  }
}

// Noon UTC, not midnight: `startOfWeek` reads the local day-of-week, and a
// UTC-midnight timestamp would land on the previous local calendar day in
// any timezone behind UTC, shifting the computed Monday by a week.
const FIXED_WEEK_START = new Date('2026-09-07T12:00:00Z')

function setup(api: IKitchenApi = fakeApi()) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/s/space-1/kitchen/menu']}>
        <Routes>
          <Route path="/s/:spaceId/kitchen/menu" element={<KitchenMenuPage api={api} initialWeekStart={FIXED_WEEK_START} />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
  return { api }
}

describe('KitchenMenuPage', () => {
  it('shows a planned meal on its day', async () => {
    setup()

    expect(await screen.findByText('Pâtes bolognaise')).toBeDefined()
  })

  it('shows the computed shopping list with a translated unit', async () => {
    setup()

    expect(await screen.findByText('Pâtes')).toBeDefined()
    expect(screen.getByText('500 unit.GRAM')).toBeDefined()
  })

  it('removes a planned meal', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getByLabelText('menu.remove_meal'))

    await waitFor(() => expect(api.removeMenuEntry).toHaveBeenCalledWith('space-1', 'e1'))
  })

  it('adds a meal via the picker, defaulting to the recipe planned longest ago', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getAllByText('+ menu.add_meal')[0])
    fireEvent.click(screen.getByText('menu.confirm_add'))

    await waitFor(() => expect(api.addMenuEntry).toHaveBeenCalledWith(
      'space-1', expect.any(String), 'r2', 4))
  })
})
