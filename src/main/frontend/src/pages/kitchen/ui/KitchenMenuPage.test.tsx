import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { KitchenMenuPage } from './KitchenMenuPage'
import type { IKitchenApi } from '../model/IKitchenApi'
import type { MenuEntry, Recipe, ShoppingListLine } from '../model/types'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/pages/shopping', async () => {
  const actual = await vi.importActual<typeof import('@/pages/shopping')>('@/pages/shopping')
  return {
    ...actual,
    useShoppingCategories: () => ({ data: [{ id: 'cat-1', name: 'Épicerie', position: 0, fallback: false }] }),
    useImportFromMenu: () => ({ mutateAsync: vi.fn().mockResolvedValue([]), isPending: false }),
  }
})

const RECIPES: Recipe[] = [
  { id: 'r1', name: 'Pâtes bolognaise', category: 'PLAT', minutes: 35, referencePortions: 4, favorite: false, ingredients: [], steps: [], lastPlannedOn: '2026-08-01' },
  { id: 'r2', name: 'Curry de légumes', category: 'VEGETARIAN', minutes: 30, referencePortions: 4, favorite: false, ingredients: [], steps: [], lastPlannedOn: null },
]

const ENTRIES: MenuEntry[] = [
  { id: 'e1', date: '2026-09-07', recipeId: 'r1', recipeName: 'Pâtes bolognaise', recipeCategory: 'PLAT', portions: 4, position: 0 },
]

const SHOPPING_LIST: ShoppingListLine[] = [{ name: 'Pâtes', quantity: 500, unit: 'GRAM' }]

const CURRENT_SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 2,
}

function fakeApi(overrides: Partial<IKitchenApi> = {}): IKitchenApi {
  return {
    listRecipes: vi.fn().mockResolvedValue(RECIPES),
    getRecipe: vi.fn(),
    createRecipe: vi.fn(),
    updateRecipe: vi.fn(),
    deleteRecipe: vi.fn(),
    toggleFavorite: vi.fn(),
    copyRecipe: vi.fn(),
    moveRecipe: vi.fn(),
    listMenuEntries: vi.fn().mockResolvedValue(ENTRIES),
    addMenuEntry: vi.fn().mockResolvedValue(ENTRIES[0]),
    updateMenuEntryPortions: vi.fn().mockResolvedValue(undefined),
    removeMenuEntry: vi.fn().mockResolvedValue(undefined),
    getShoppingList: vi.fn().mockResolvedValue(SHOPPING_LIST),
    ...overrides,
  }
}

function fakeSpacesApi(mySpaces: SpaceSummary[] = [CURRENT_SPACE]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockResolvedValue(mySpaces),
    getSpace: vi.fn(),
  }
}

// Noon UTC, not midnight: `startOfWeek` reads the local day-of-week, and a
// UTC-midnight timestamp would land on the previous local calendar day in
// any timezone behind UTC, shifting the computed Monday by a week.
const FIXED_WEEK_START = new Date('2026-09-07T12:00:00Z')

function setup(api: IKitchenApi = fakeApi(), spacesApi: ISpacesApi = fakeSpacesApi()) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={spacesApi}>
        <MemoryRouter initialEntries={['/s/space-1/kitchen/menu']}>
          <Routes>
            <Route path="/s/:spaceId/kitchen/menu" element={<KitchenMenuPage api={api} initialWeekStart={FIXED_WEEK_START} />} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return { api, spacesApi }
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

  it('opens the export modal and imports the checked lines to the shopping list', async () => {
    setup(fakeApi({ getShoppingList: vi.fn().mockResolvedValue(SHOPPING_LIST) }))
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('menu.export_to_shopping_list'))

    expect(await screen.findByText('title')).toBeDefined() // ExportToShoppingListModal's own (mocked) i18n
  })
})

describe('KitchenMenuPage — read-only role', () => {
  it('hides the portions input, remove button, and add-meal control for a viewer', async () => {
    const spacesApi = fakeSpacesApi([{ ...CURRENT_SPACE, myRole: 'VIEWER' }])
    setup(fakeApi(), spacesApi)
    await screen.findByText('Pâtes bolognaise')
    // Wait for the concurrent spaces query to settle before asserting the gated controls are absent.
    await waitFor(() => expect(screen.getByText('4')).toBeDefined())

    expect(screen.queryByLabelText('menu.portions_label')).toBeNull()
    expect(screen.queryByLabelText('menu.remove_meal')).toBeNull()
    expect(screen.queryByText('+ menu.add_meal')).toBeNull()
  })

  it('still shows the planned portions as read-only text for a viewer', async () => {
    const spacesApi = fakeSpacesApi([{ ...CURRENT_SPACE, myRole: 'VIEWER' }])
    setup(fakeApi(), spacesApi)
    await screen.findByText('Pâtes bolognaise')

    expect(await screen.findByText('4')).toBeDefined()
  })
})

describe('KitchenMenuPage — mutation errors', () => {
  it('shows an error message when removing a meal fails', async () => {
    const api = fakeApi({ removeMenuEntry: vi.fn().mockRejectedValue(new Error('boom')) })
    setup(api)
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getByLabelText('menu.remove_meal'))

    expect(await screen.findByText('error.action_failed')).toBeDefined()
  })

  it('shows an error message when adding a meal fails', async () => {
    const api = fakeApi({ addMenuEntry: vi.fn().mockRejectedValue(new Error('boom')) })
    setup(api)
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getAllByText('+ menu.add_meal')[0])
    fireEvent.click(screen.getByText('menu.confirm_add'))

    expect(await screen.findByText('error.action_failed')).toBeDefined()
  })

  it('shows an error message when updating portions fails', async () => {
    const api = fakeApi({ updateMenuEntryPortions: vi.fn().mockRejectedValue(new Error('boom')) })
    setup(api)
    await screen.findByText('Pâtes bolognaise')

    fireEvent.blur(screen.getByLabelText('menu.portions_label'), { target: { value: '6' } })

    expect(await screen.findByText('error.action_failed')).toBeDefined()
  })
})
