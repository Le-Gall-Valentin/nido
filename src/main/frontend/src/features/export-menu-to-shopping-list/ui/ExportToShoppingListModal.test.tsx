import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { ShoppingListLine } from '@/pages/kitchen'
import { ExportToShoppingListModal } from './ExportToShoppingListModal'
import type { IShoppingApi } from '@/pages/shopping'
import type { ShoppingCategory } from '@/pages/shopping'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const LINES: ShoppingListLine[] = [
  { name: 'Poulet', quantity: 800, unit: 'GRAM' },
  { name: 'Riz', quantity: 300, unit: 'GRAM' },
]

const CATEGORIES: ShoppingCategory[] = [
  { id: 'cat-1', name: 'Épicerie', position: 0, fallback: false },
  { id: 'cat-2', name: 'Frais', position: 1, fallback: false },
]

function fakeApi(overrides: Partial<IShoppingApi> = {}): IShoppingApi {
  return {
    listCategories: vi.fn().mockResolvedValue(CATEGORIES),
    createCategory: vi.fn(),
    renameCategory: vi.fn(),
    deleteCategory: vi.fn(),
    listItems: vi.fn(),
    addItem: vi.fn(),
    updateItem: vi.fn(),
    toggleItemDone: vi.fn(),
    deleteItem: vi.fn(),
    clearDoneItems: vi.fn(),
    clearAllItems: vi.fn(),
    importFromMenu: vi.fn().mockResolvedValue([]),
    ...overrides,
  }
}

function setup(api: IShoppingApi = fakeApi(), onClose = vi.fn(), onImported = vi.fn()) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ExportToShoppingListModal
          open onClose={onClose} spaceId="space-1" shoppingList={LINES} onImported={onImported} api={api}
        />
      </MemoryRouter>
    </QueryClientProvider>
  )
  return { api, onClose, onImported }
}

// The mocked `t` above discards the interpolation object entirely, so
// `t('include_line', { name: 'Poulet' })` and `t('include_line', { name: 'Riz' })`
// both resolve to the literal key `'include_line'` — every row's checkbox (and
// every row's category select) shares that one label. Select by position
// instead, in the same order as LINES (Poulet = index 0, Riz = index 1).

describe('ExportToShoppingListModal', () => {
  it('lists every suggested ingredient, checked by default, with its formatted quantity', async () => {
    setup()

    expect(await screen.findByText('Poulet')).toBeDefined()
    expect(screen.getByDisplayValue('800 unit.GRAM')).toBeDefined()
    expect(screen.getAllByLabelText('include_line')[0]).toHaveProperty('checked', true)
  })

  it('unchecking a line excludes it from the import', async () => {
    const { api, onImported } = setup()
    await screen.findByText('Poulet')

    fireEvent.click(screen.getAllByLabelText('include_line')[0])
    fireEvent.click(screen.getByText('confirm'))

    await waitFor(() => expect(api.importFromMenu).toHaveBeenCalledWith('space-1', [
      { name: 'Riz', quantityLabel: '300 unit.GRAM', categoryId: 'cat-1' },
    ]))
    expect(onImported).toHaveBeenCalled()
  })

  it('the bulk category selector applies to every row', async () => {
    const { api } = setup()
    await screen.findByText('Poulet')
    // The bulk selector only renders once the categories query resolves —
    // that's asynchronous and independent of `shoppingList` (a prop), which
    // is why "Poulet" can already be on screen before this appears.
    const bulkSelect = await screen.findByLabelText('bulk_category_label')

    fireEvent.change(bulkSelect, { target: { value: 'cat-2' } })
    fireEvent.click(screen.getByText('confirm'))

    await waitFor(() => expect(api.importFromMenu).toHaveBeenCalledWith('space-1', [
      { name: 'Poulet', quantityLabel: '800 unit.GRAM', categoryId: 'cat-2' },
      { name: 'Riz', quantityLabel: '300 unit.GRAM', categoryId: 'cat-2' },
    ]))
  })

  it('a per-row category override survives the bulk selector applied earlier', async () => {
    const { api } = setup()
    await screen.findByText('Poulet')
    const bulkSelect = await screen.findByLabelText('bulk_category_label')

    fireEvent.change(bulkSelect, { target: { value: 'cat-2' } })
    fireEvent.change(screen.getAllByLabelText('category_for')[0], { target: { value: 'cat-1' } })
    fireEvent.click(screen.getByText('confirm'))

    await waitFor(() => expect(api.importFromMenu).toHaveBeenCalledWith('space-1', [
      { name: 'Poulet', quantityLabel: '800 unit.GRAM', categoryId: 'cat-1' },
      { name: 'Riz', quantityLabel: '300 unit.GRAM', categoryId: 'cat-2' },
    ]))
  })

  it('editing a quantity draft sends the edited text', async () => {
    const { api } = setup()
    await screen.findByText('Poulet')

    fireEvent.change(screen.getByDisplayValue('800 unit.GRAM'), { target: { value: '1 kg' } })
    fireEvent.click(screen.getByText('confirm'))

    await waitFor(() => expect(api.importFromMenu).toHaveBeenCalledWith('space-1', expect.arrayContaining([
      { name: 'Poulet', quantityLabel: '1 kg', categoryId: 'cat-1' },
    ])))
  })

  it('shows an error message when the import fails', async () => {
    const api = fakeApi({ importFromMenu: vi.fn().mockRejectedValue(new Error('boom')) })
    setup(api)
    await screen.findByText('Poulet')

    fireEvent.click(screen.getByText('confirm'))

    expect(await screen.findByText('error')).toBeDefined()
  })
})
