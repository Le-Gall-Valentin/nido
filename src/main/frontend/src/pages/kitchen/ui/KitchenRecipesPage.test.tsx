import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { KitchenRecipesPage } from './KitchenRecipesPage'
import type { IKitchenApi } from '../model/IKitchenApi'
import type { Recipe } from '../model/types'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const RECIPES: Recipe[] = [
  { id: 'r1', name: 'Pâtes bolognaise', category: 'PLAT', minutes: 35, referencePortions: 4, favorite: false, ingredients: [{ name: 'Pâtes', quantity: 400, unit: 'GRAM' }], steps: [] },
  { id: 'r2', name: 'Curry de légumes', category: 'VEGETARIAN', minutes: 30, referencePortions: 4, favorite: true, ingredients: [], steps: [] },
]

const CURRENT_SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 2,
}
const OTHER_SPACE: SpaceSummary = {
  id: 'space-2', type: 'PERSONAL', name: 'Perso', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}

function fakeApi(overrides: Partial<IKitchenApi> = {}): IKitchenApi {
  return {
    listRecipes: vi.fn().mockResolvedValue(RECIPES),
    getRecipe: vi.fn(),
    createRecipe: vi.fn(),
    updateRecipe: vi.fn(),
    deleteRecipe: vi.fn().mockResolvedValue(undefined),
    toggleFavorite: vi.fn().mockResolvedValue(RECIPES[0]),
    copyRecipe: vi.fn().mockResolvedValue(RECIPES[0]),
    moveRecipe: vi.fn().mockResolvedValue(RECIPES[0]),
    listMenuEntries: vi.fn(),
    addMenuEntry: vi.fn(),
    updateMenuEntryPortions: vi.fn(),
    removeMenuEntry: vi.fn(),
    getShoppingList: vi.fn(),
    ...overrides,
  }
}

function fakeSpacesApi(mySpaces: SpaceSummary[] = [CURRENT_SPACE, OTHER_SPACE]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockResolvedValue(mySpaces),
    getSpace: vi.fn(),
  }
}

function setup(api: IKitchenApi = fakeApi(), spacesApi: ISpacesApi = fakeSpacesApi()) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={spacesApi}>
        <MemoryRouter initialEntries={['/s/space-1/kitchen/recipes']}>
          <Routes>
            <Route path="/s/:spaceId/kitchen/recipes" element={<KitchenRecipesPage api={api} />} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return { api, spacesApi }
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

  it('deletes a recipe after confirming', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getAllByText('delete')[0])
    expect(api.deleteRecipe).not.toHaveBeenCalled()
    fireEvent.click(screen.getByText('delete_confirm.submit'))

    await waitFor(() => expect(api.deleteRecipe).toHaveBeenCalledWith('space-1', 'r1'))
  })

  it('does not delete when the confirmation is cancelled', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getAllByText('delete')[0])
    fireEvent.click(screen.getByText('delete_confirm.cancel'))

    expect(api.deleteRecipe).not.toHaveBeenCalled()
    expect(screen.queryByText('delete_confirm.submit')).toBeNull()
  })

  it('edits a recipe', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getAllByText('edit')[0])
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(api.updateRecipe).toHaveBeenCalledWith('space-1', 'r1', expect.objectContaining({ name: 'Pâtes bolognaise' })))
  })

  it('toggles favorite', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    fireEvent.click(screen.getAllByLabelText('favorite')[0])

    await waitFor(() => expect(api.toggleFavorite).toHaveBeenCalledWith('space-1', 'r1'))
  })

  it('copies a recipe to the chosen destination', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    const copyButtons = await screen.findAllByText('transfer.copy_submit:{"ns":"common"}')
    fireEvent.click(copyButtons[0])
    fireEvent.click(await screen.findByText('Perso'))
    fireEvent.click(screen.getByText('transfer.copy_submit'))

    await waitFor(() => expect(api.copyRecipe).toHaveBeenCalledWith('space-1', 'r1', 'space-2'))
  })

  it('moves a recipe to the chosen destination', async () => {
    const { api } = setup()
    await screen.findByText('Pâtes bolognaise')

    const moveButtons = await screen.findAllByText('transfer.move_submit:{"ns":"common"}')
    fireEvent.click(moveButtons[0])
    fireEvent.click(await screen.findByText('Perso'))
    fireEvent.click(screen.getByText('transfer.move_submit'))

    await waitFor(() => expect(api.moveRecipe).toHaveBeenCalledWith('space-1', 'r1', 'space-2'))
  })
})

describe('KitchenRecipesPage — read-only role', () => {
  it('hides create, edit, delete, and move for a viewer, but still offers copy', async () => {
    const spacesApi = fakeSpacesApi([{ ...CURRENT_SPACE, myRole: 'VIEWER' }, OTHER_SPACE])
    setup(fakeApi(), spacesApi)
    await screen.findByText('Pâtes bolognaise')
    // "Copier" is never role-gated; waiting for it also forces the concurrent
    // spaces query to settle before the role-gated buttons are asserted absent.
    const copyButtons = await screen.findAllByText('transfer.copy_submit:{"ns":"common"}')

    expect(copyButtons.length).toBeGreaterThan(0)
    expect(screen.queryByText('recipes.new_recipe')).toBeNull()
    expect(screen.queryByText('edit')).toBeNull()
    expect(screen.queryByText('delete')).toBeNull()
    expect(screen.queryByText('transfer.move_submit:{"ns":"common"}')).toBeNull()
  })
})
