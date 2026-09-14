import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { ShoppingListPage } from './ShoppingListPage'
import type { IShoppingApi, ShoppingCategory, ShoppingItem } from '@/entities/shopping-list'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const CATEGORIES: ShoppingCategory[] = [
  { id: 'cat-1', name: 'Épicerie', position: 0, fallback: false },
  { id: 'cat-2', name: 'Maison & divers', position: 1, fallback: true },
]

const ITEMS: ShoppingItem[] = [
  { id: 'i1', categoryId: 'cat-1', name: 'Pâtes', quantity: 500, unit: 'GRAM', done: false, position: 0 },
]

const CURRENT_SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 2, timezone: 'Europe/Paris',
}

function fakeApi(overrides: Partial<IShoppingApi> = {}): IShoppingApi {
  return {
    listCategories: vi.fn().mockResolvedValue(CATEGORIES),
    createCategory: vi.fn().mockResolvedValue(undefined),
    renameCategory: vi.fn().mockResolvedValue(undefined),
    deleteCategory: vi.fn().mockResolvedValue(undefined),
    listItems: vi.fn().mockResolvedValue(ITEMS),
    addItem: vi.fn().mockResolvedValue(undefined),
    updateItem: vi.fn(),
    toggleItemDone: vi.fn().mockResolvedValue(undefined),
    deleteItem: vi.fn().mockResolvedValue(undefined),
    clearDoneItems: vi.fn().mockResolvedValue(undefined),
    clearAllItems: vi.fn().mockResolvedValue(undefined),
    importFromMenu: vi.fn(),
    ...overrides,
  }
}

function fakeSpacesApi(mySpaces: SpaceSummary[] = [CURRENT_SPACE]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockResolvedValue(mySpaces),
    getSpace: vi.fn(),
  }
}

function setup(api: IShoppingApi = fakeApi(), spacesApi: ISpacesApi = fakeSpacesApi()) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={spacesApi}>
        <MemoryRouter initialEntries={['/s/space-1/organisation/courses']}>
          <Routes>
            <Route path="/s/:spaceId/organisation/courses" element={<ShoppingListPage api={api} />} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return { api, spacesApi }
}

describe('ShoppingListPage', () => {
  it('shows items grouped under their category', async () => {
    setup()

    expect(await screen.findByText('Épicerie')).toBeDefined()
    expect(screen.getByText('Pâtes')).toBeDefined()
  })

  it('leaves a category holding no item out of the list entirely', async () => {
    setup()
    await screen.findByText('Pâtes')

    // cat-2 holds nothing, and with both modals closed its name appears nowhere on the page —
    // not as a header, not as a <select> option.
    expect(screen.queryByText('Maison & divers')).toBeNull()
  })

  it('adds an item through the add-item modal', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('add_item'))
    fireEvent.change(screen.getByLabelText('add_item_name_label'), { target: { value: 'Riz' } })
    fireEvent.change(screen.getByLabelText('quantity_label'), { target: { value: '300' } })
    fireEvent.change(screen.getByLabelText('unit_label'), { target: { value: 'GRAM' } })
    fireEvent.click(screen.getByText('add_item_confirm'))

    await waitFor(() => expect(api.addItem).toHaveBeenCalledWith('space-1', 'cat-1', 'Riz', 300, 'GRAM'))
    await waitFor(() => expect(screen.queryByLabelText('add_item_name_label')).toBeNull())
  })

  it('offers every category in the add-item modal, the empty ones included', async () => {
    setup()
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('add_item'))

    const select = screen.getByLabelText('category_label')
    expect(within(select).getByText('Maison & divers')).toBeDefined()
  })

  it('moves an item to a category holding no item, preserving its quantity and unit', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    // The only way into an empty category, now that empty ones are not drop targets on the list.
    fireEvent.click(screen.getByLabelText('move_item'))
    const dialog = screen.getByRole('dialog')
    fireEvent.click(within(dialog).getByText('Maison & divers'))

    await waitFor(() => expect(api.updateItem).toHaveBeenCalledWith('space-1', 'i1', 'cat-2', 'Pâtes', 500, 'GRAM'))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('disables the item\'s current category in the move dialog', async () => {
    setup()
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByLabelText('move_item'))
    const dialog = screen.getByRole('dialog')

    expect(within(dialog).getByText('move_item_target_current')).toHaveProperty('disabled', true)
  })

  it('toggles an item done', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByLabelText('toggle_done'))

    await waitFor(() => expect(api.toggleItemDone).toHaveBeenCalledWith('space-1', 'i1'))
  })

  it('deletes an item', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByLabelText('delete_item'))

    await waitFor(() => expect(api.deleteItem).toHaveBeenCalledWith('space-1', 'i1'))
  })

  it('clears done items', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('clear_done'))

    await waitFor(() => expect(api.clearDoneItems).toHaveBeenCalledWith('space-1'))
  })

  it('clears all items', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('clear_all'))

    await waitFor(() => expect(api.clearAllItems).toHaveBeenCalledWith('space-1'))
  })

  it('offers no way to rename or delete a category from the list itself', async () => {
    setup()
    await screen.findByText('Pâtes')

    expect(screen.queryByLabelText('category_rename_for')).toBeNull()
    expect(screen.queryByLabelText('category_delete')).toBeNull()
  })
})

describe('ShoppingListPage — managing categories', () => {
  it('renames a category through the manage-categories modal', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('manage_categories'))
    // Every row's rename control shares an accessible name here, because the mocked t() drops the
    // interpolated category name. cat-1 is first in position order.
    fireEvent.click(screen.getAllByLabelText('category_rename_for')[0]!)
    fireEvent.change(screen.getByLabelText('category_rename'), { target: { value: 'Épicerie fine' } })
    fireEvent.click(screen.getByLabelText('category_rename_confirm'))

    await waitFor(() => expect(api.renameCategory).toHaveBeenCalledWith('space-1', 'cat-1', 'Épicerie fine'))
  })

  it('creates a category through the manage-categories modal', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('manage_categories'))
    fireEvent.change(screen.getByLabelText('new_category_placeholder'), { target: { value: 'Bricolage' } })
    fireEvent.click(screen.getByText('new_category'))

    await waitFor(() => expect(api.createCategory).toHaveBeenCalledWith('space-1', 'Bricolage'))
  })

  it('deletes a category through the manage-categories modal, but never the fallback one', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('manage_categories'))
    // Two categories, one bin: the fallback category offers none.
    expect(screen.getAllByLabelText('category_delete')).toHaveLength(1)
    fireEvent.click(screen.getByLabelText('category_delete'))
    fireEvent.click(screen.getByText('category_delete_confirm_action'))

    await waitFor(() => expect(api.deleteCategory).toHaveBeenCalledWith('space-1', 'cat-1'))
  })
})

describe('ShoppingListPage — read-only role', () => {
  it('hides every write control for a viewer', async () => {
    const spacesApi = fakeSpacesApi([{ ...CURRENT_SPACE, myRole: 'VIEWER' }])
    setup(fakeApi(), spacesApi)
    await screen.findByText('Pâtes')

    expect(screen.queryByText('add_item')).toBeNull()
    expect(screen.queryByText('manage_categories')).toBeNull()
    expect(screen.queryByLabelText('move_item')).toBeNull()
    expect(screen.queryByLabelText('delete_item')).toBeNull()
  })
})

describe('ShoppingListPage — mutation errors', () => {
  it('shows an error message when moving an item to another category fails', async () => {
    const api = fakeApi({ updateItem: vi.fn().mockRejectedValue(new Error('boom')) })
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByLabelText('move_item'))
    fireEvent.click(within(screen.getByRole('dialog')).getByText('Maison & divers'))

    expect(await screen.findByText('error.action_failed')).toBeDefined()
  })

  it('shows an error message when deleting an item fails', async () => {
    const api = fakeApi({ deleteItem: vi.fn().mockRejectedValue(new Error('boom')) })
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByLabelText('delete_item'))

    expect(await screen.findByText('error.action_failed')).toBeDefined()
  })
})
