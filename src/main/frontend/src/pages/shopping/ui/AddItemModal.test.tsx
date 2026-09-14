import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AddItemModal } from './AddItemModal'
import type { ShoppingCategory } from '@/entities/shopping-list'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const CATEGORIES: ShoppingCategory[] = [
  { id: 'cat-1', name: 'Épicerie', position: 0, fallback: false },
  { id: 'cat-2', name: 'Maison & divers', position: 1, fallback: true },
]

function setup(onAdd = vi.fn().mockResolvedValue(undefined), onClose = vi.fn()) {
  render(<AddItemModal categories={CATEGORIES} onAdd={onAdd} onClose={onClose} />)
  return { onAdd, onClose }
}

describe('AddItemModal', () => {
  it('adds an item to the first category by default and closes', async () => {
    const { onAdd, onClose } = setup()

    fireEvent.change(screen.getByLabelText('add_item_name_label'), { target: { value: 'Riz' } })
    fireEvent.click(screen.getByText('add_item_confirm'))

    await waitFor(() => expect(onAdd).toHaveBeenCalledWith({
      categoryId: 'cat-1', name: 'Riz', quantity: null, unit: null,
    }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
  })

  it('adds an item with a quantity, a unit and a chosen category', async () => {
    const { onAdd } = setup()

    fireEvent.change(screen.getByLabelText('add_item_name_label'), { target: { value: 'Farine' } })
    fireEvent.change(screen.getByLabelText('quantity_label'), { target: { value: '300' } })
    fireEvent.change(screen.getByLabelText('unit_label'), { target: { value: 'GRAM' } })
    fireEvent.change(screen.getByLabelText('category_label'), { target: { value: 'cat-2' } })
    fireEvent.click(screen.getByText('add_item_confirm'))

    await waitFor(() => expect(onAdd).toHaveBeenCalledWith({
      categoryId: 'cat-2', name: 'Farine', quantity: 300, unit: 'GRAM',
    }))
  })

  it('trims the name before adding', async () => {
    const { onAdd } = setup()

    fireEvent.change(screen.getByLabelText('add_item_name_label'), { target: { value: '  Riz  ' } })
    fireEvent.click(screen.getByText('add_item_confirm'))

    await waitFor(() => expect(onAdd).toHaveBeenCalledWith(expect.objectContaining({ name: 'Riz' })))
  })

  it('does not add anything when the name is blank', () => {
    const { onAdd, onClose } = setup()

    fireEvent.change(screen.getByLabelText('add_item_name_label'), { target: { value: '   ' } })
    fireEvent.click(screen.getByText('add_item_confirm'))

    expect(onAdd).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('shows the quantity error inside the modal and stays open', async () => {
    const { onAdd, onClose } = setup()

    fireEvent.change(screen.getByLabelText('add_item_name_label'), { target: { value: 'Riz' } })
    fireEvent.change(screen.getByLabelText('quantity_label'), { target: { value: '-5' } })
    fireEvent.click(screen.getByText('add_item_confirm'))

    expect(await screen.findByText('error.quantity_invalid')).toBeDefined()
    expect(onAdd).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('shows an error inside the modal and stays open when adding fails', async () => {
    const { onClose } = setup(vi.fn().mockRejectedValue(new Error('boom')))

    fireEvent.change(screen.getByLabelText('add_item_name_label'), { target: { value: 'Riz' } })
    fireEvent.click(screen.getByText('add_item_confirm'))

    expect(await screen.findByText('error.action_failed')).toBeDefined()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('closes without adding when cancelled', () => {
    const { onAdd, onClose } = setup()

    fireEvent.change(screen.getByLabelText('add_item_name_label'), { target: { value: 'Riz' } })
    fireEvent.click(screen.getByText('form_cancel'))

    expect(onClose).toHaveBeenCalled()
    expect(onAdd).not.toHaveBeenCalled()
  })

  it('submits on Enter in the name field', async () => {
    const { onAdd } = setup()

    const name = screen.getByLabelText('add_item_name_label')
    fireEvent.change(name, { target: { value: 'Riz' } })
    fireEvent.keyDown(name, { key: 'Enter' })

    await waitFor(() => expect(onAdd).toHaveBeenCalled())
  })
})
