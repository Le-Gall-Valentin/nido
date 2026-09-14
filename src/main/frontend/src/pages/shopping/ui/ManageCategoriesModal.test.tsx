import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, type Mock } from 'vitest'
import { ManageCategoriesModal } from './ManageCategoriesModal'
import type { ShoppingCategory } from '@/entities/shopping-list'

// Interpolating on purpose: the deletion notice's whole job is to name the fallback category and
// count the items going there, and a mock that returns the bare key would hide both.
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, params?: Record<string, unknown>) => (params
      ? `${key}(${Object.entries(params).map(([k, v]) => `${k}=${String(v)}`).join(',')})`
      : key),
  }),
}))

const CATEGORIES: ShoppingCategory[] = [
  { id: 'cat-1', name: 'Épicerie', position: 0, fallback: false },
  { id: 'cat-2', name: 'Maison & divers', position: 1, fallback: true },
  { id: 'cat-3', name: 'Bricolage', position: 2, fallback: false },
]

const ITEM_COUNTS = new Map([['cat-1', 2], ['cat-2', 1], ['cat-3', 3]])

interface Handlers {
  onCreate?: Mock<(name: string) => Promise<unknown>>
  onRename?: Mock<(categoryId: string, name: string) => Promise<unknown>>
  onDelete?: Mock<(categoryId: string) => Promise<unknown>>
  onClose?: Mock<() => void>
}

function setup(handlers: Handlers = {}) {
  const onCreate = handlers.onCreate ?? vi.fn<(name: string) => Promise<unknown>>().mockResolvedValue(undefined)
  const onRename = handlers.onRename ?? vi.fn<(categoryId: string, name: string) => Promise<unknown>>().mockResolvedValue(undefined)
  const onDelete = handlers.onDelete ?? vi.fn<(categoryId: string) => Promise<unknown>>().mockResolvedValue(undefined)
  const onClose = handlers.onClose ?? vi.fn<() => void>()
  render(
    <ManageCategoriesModal
      categories={CATEGORIES} itemCountByCategory={ITEM_COUNTS}
      onCreate={onCreate} onRename={onRename} onDelete={onDelete} onClose={onClose}
    />
  )
  return { onCreate, onRename, onDelete, onClose }
}

describe('ManageCategoriesModal', () => {
  it('lists every category, including the ones holding no item', () => {
    setup()

    expect(screen.getByText('Épicerie')).toBeDefined()
    expect(screen.getByText('Maison & divers')).toBeDefined()
    expect(screen.getByText('Bricolage')).toBeDefined()
  })

  it('renames a category', async () => {
    const { onRename } = setup()

    fireEvent.click(screen.getByLabelText('category_rename_for(name=Épicerie)'))
    fireEvent.change(screen.getByLabelText('category_rename'), { target: { value: 'Épicerie fine' } })
    fireEvent.click(screen.getByLabelText('category_rename_confirm'))

    await waitFor(() => expect(onRename).toHaveBeenCalledWith('cat-1', 'Épicerie fine'))
    await waitFor(() => expect(screen.queryByLabelText('category_rename')).toBeNull())
  })

  it('renames the fallback category too', async () => {
    const { onRename } = setup()

    fireEvent.click(screen.getByLabelText('category_rename_for(name=Maison & divers)'))
    fireEvent.change(screen.getByLabelText('category_rename'), { target: { value: 'Divers' } })
    fireEvent.click(screen.getByLabelText('category_rename_confirm'))

    await waitFor(() => expect(onRename).toHaveBeenCalledWith('cat-2', 'Divers'))
  })

  it('leaves the name untouched when a rename is cancelled', () => {
    const { onRename } = setup()

    fireEvent.click(screen.getByLabelText('category_rename_for(name=Épicerie)'))
    fireEvent.change(screen.getByLabelText('category_rename'), { target: { value: 'Autre' } })
    fireEvent.click(screen.getByLabelText('form_cancel'))

    expect(onRename).not.toHaveBeenCalled()
    expect(screen.getByText('Épicerie')).toBeDefined()
  })

  it('does not rename to a blank name', () => {
    const { onRename } = setup()

    fireEvent.click(screen.getByLabelText('category_rename_for(name=Épicerie)'))
    fireEvent.change(screen.getByLabelText('category_rename'), { target: { value: '  ' } })
    fireEvent.click(screen.getByLabelText('category_rename_confirm'))

    expect(onRename).not.toHaveBeenCalled()
  })

  it('creates a category and clears the field', async () => {
    const { onCreate } = setup()

    fireEvent.change(screen.getByLabelText('new_category_placeholder'), { target: { value: 'Jardin' } })
    fireEvent.click(screen.getByText('new_category'))

    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('Jardin'))
    await waitFor(() => expect(screen.getByLabelText('new_category_placeholder')).toHaveProperty('value', ''))
  })

  it('does not create a category with a blank name', () => {
    const { onCreate } = setup()

    fireEvent.change(screen.getByLabelText('new_category_placeholder'), { target: { value: '   ' } })
    fireEvent.click(screen.getByText('new_category'))

    expect(onCreate).not.toHaveBeenCalled()
  })

  it('asks to confirm a deletion, naming the fallback category and counting the items moved there', () => {
    const { onDelete } = setup()

    fireEvent.click(screen.getByLabelText('category_delete(name=Bricolage)'))

    expect(screen.getByText('category_delete_confirm(name=Bricolage)')).toBeDefined()
    expect(screen.getByText('category_delete_reassign(count=3,fallback=Maison & divers)')).toBeDefined()
    expect(onDelete).not.toHaveBeenCalled()
  })

  it('deletes the category once confirmed', async () => {
    const { onDelete } = setup()

    fireEvent.click(screen.getByLabelText('category_delete(name=Bricolage)'))
    fireEvent.click(screen.getByText('category_delete_confirm_action'))

    await waitFor(() => expect(onDelete).toHaveBeenCalledWith('cat-3'))
  })

  it('abandons the deletion when the confirmation is cancelled', () => {
    const { onDelete } = setup()

    fireEvent.click(screen.getByLabelText('category_delete(name=Bricolage)'))
    fireEvent.click(screen.getByText('form_cancel'))

    expect(onDelete).not.toHaveBeenCalled()
    expect(screen.queryByText('category_delete_confirm(name=Bricolage)')).toBeNull()
  })

  it('offers no deletion for the fallback category, and says why on its name', () => {
    setup()

    expect(screen.queryByLabelText('category_delete(name=Maison & divers)')).toBeNull()
    expect(screen.getByText('category_fallback_hint')).toBeDefined()
  })

  it('counts the items of every category in the same place, the fallback one included', () => {
    setup()

    // One reading per row, in one column: what a category *holds*. What it *is* — the fallback
    // marker — sits by the name instead, so neither reading has to displace the other.
    expect(screen.getByText('category_item_count(count=2)')).toBeDefined()
    expect(screen.getByText('category_item_count(count=1)')).toBeDefined()
    expect(screen.getByText('category_item_count(count=3)')).toBeDefined()
    expect(screen.queryAllByText('category_item_count(count=0)')).toHaveLength(0)
  })

  it('shows an error when a mutation fails', async () => {
    setup({ onCreate: vi.fn<(name: string) => Promise<unknown>>().mockRejectedValue(new Error('boom')) })

    fireEvent.change(screen.getByLabelText('new_category_placeholder'), { target: { value: 'Jardin' } })
    fireEvent.click(screen.getByText('new_category'))

    expect(await screen.findByText('error.action_failed')).toBeDefined()
  })

  it('closes on the close button', () => {
    const { onClose } = setup()

    fireEvent.click(screen.getByText('form_close'))

    expect(onClose).toHaveBeenCalled()
  })
})
