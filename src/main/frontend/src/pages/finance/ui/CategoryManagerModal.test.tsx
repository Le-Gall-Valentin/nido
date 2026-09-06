import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { CategoryManagerModal } from './CategoryManagerModal'
import type { Category } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const categories: Category[] = [
  { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true, type: 'EXPENSE' },
  { id: 'c2', label: 'Animaux', color: '#a3e635', icon: 'PawPrint', isDefault: false, type: 'EXPENSE' },
]

describe('CategoryManagerModal', () => {
  it('shows the submit error handed down by the caller when the backend rejected a create or update', () => {
    render(<CategoryManagerModal categories={categories} onCreate={vi.fn()} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} submitError="form.submit_error" />)

    expect(screen.getByText('form.submit_error')).toBeDefined()
  })

  it('lists every category', () => {
    render(<CategoryManagerModal categories={categories} onCreate={vi.fn()} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    expect(screen.getByText('Alimentation')).toBeDefined()
    expect(screen.getByText('Animaux')).toBeDefined()
  })

  it('creates a new category from the add form', () => {
    const onCreate = vi.fn()
    render(<CategoryManagerModal categories={categories} onCreate={onCreate} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.change(screen.getByLabelText('categories.new_category_label'), { target: { value: 'Sport' } })
    fireEvent.click(screen.getByText('categories.add'))

    expect(onCreate).toHaveBeenCalledWith('Sport', expect.any(String), expect.any(String), 'EXPENSE')
  })

  it('creates an income category when Revenu is selected', () => {
    const onCreate = vi.fn()
    render(<CategoryManagerModal categories={categories} onCreate={onCreate} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.change(screen.getByLabelText('categories.new_category_label'), { target: { value: 'Prime' } })
    fireEvent.click(screen.getByText('type.INCOME'))
    fireEvent.click(screen.getByText('categories.add'))

    expect(onCreate).toHaveBeenCalledWith('Prime', expect.any(String), expect.any(String), 'INCOME')
  })

  it('clears the new-category field once the create request succeeds', async () => {
    const onCreate = vi.fn().mockResolvedValue(undefined)
    render(<CategoryManagerModal categories={categories} onCreate={onCreate} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.change(screen.getByLabelText('categories.new_category_label'), { target: { value: 'Sport' } })
    fireEvent.click(screen.getByText('categories.add'))

    await waitFor(() => expect((screen.getByLabelText('categories.new_category_label') as HTMLInputElement).value).toBe(''))
  })

  it('keeps what the user typed in the new-category field when the create request fails', async () => {
    const onCreate = vi.fn().mockRejectedValue(new Error('boom'))
    render(<CategoryManagerModal categories={categories} onCreate={onCreate} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.change(screen.getByLabelText('categories.new_category_label'), { target: { value: 'Sport' } })
    fireEvent.click(screen.getByText('categories.add'))

    await waitFor(() => expect(onCreate).toHaveBeenCalled())
    expect((screen.getByLabelText('categories.new_category_label') as HTMLInputElement).value).toBe('Sport')
  })

  it('keeps edit mode open with the user\'s edited value when the update request fails', async () => {
    const onUpdate = vi.fn().mockRejectedValue(new Error('boom'))
    render(<CategoryManagerModal categories={categories} onCreate={vi.fn()} onUpdate={onUpdate} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.click(screen.getAllByRole('button', { name: 'categories.edit' })[0])
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(onUpdate).toHaveBeenCalled())
    // Still in edit mode (not reverted to the read-only row) with the edited value intact.
    expect(screen.getByDisplayValue('Alimentation')).toBeDefined()
  })

  it('requests deletion of a category', () => {
    const onDelete = vi.fn()
    render(<CategoryManagerModal categories={categories} onCreate={vi.fn()} onUpdate={vi.fn()} onDelete={onDelete} onClose={vi.fn()} deleteError={null} />)

    fireEvent.click(screen.getAllByRole('button', { name: 'categories.delete' })[0])

    expect(onDelete).toHaveBeenCalledWith('c1')
  })

  it('shows the delete error when one is provided', () => {
    render(<CategoryManagerModal categories={categories} onCreate={vi.fn()} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError="in_use" />)

    expect(screen.getByText('categories.delete_in_use')).toBeDefined()
  })

  it('creates a new category with an icon chosen from the appearance picker', () => {
    const onCreate = vi.fn()
    render(<CategoryManagerModal categories={categories} onCreate={onCreate} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.change(screen.getByLabelText('categories.new_category_label'), { target: { value: 'Sport' } })
    fireEvent.click(screen.getByLabelText('categories.choose_appearance'))
    fireEvent.change(screen.getByLabelText('categories.icon_search_label'), { target: { value: 'circle' } })
    fireEvent.click(screen.getByRole('button', { name: 'Circle' }))
    fireEvent.click(screen.getByText('categories.icon_confirm'))
    fireEvent.click(screen.getByText('categories.add'))

    expect(onCreate).toHaveBeenCalledWith('Sport', expect.any(String), 'Circle', 'EXPENSE')
  })

  it('creates a new category with a color chosen from the appearance picker', () => {
    const onCreate = vi.fn()
    render(<CategoryManagerModal categories={categories} onCreate={onCreate} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.change(screen.getByLabelText('categories.new_category_label'), { target: { value: 'Sport' } })
    fireEvent.click(screen.getByLabelText('categories.choose_appearance'))
    fireEvent.change(screen.getByLabelText('categories.color_label'), { target: { value: '#112233' } })
    fireEvent.click(screen.getByText('categories.icon_confirm'))
    fireEvent.click(screen.getByText('categories.add'))

    expect(onCreate).toHaveBeenCalledWith('Sport', '#112233', expect.any(String), 'EXPENSE')
  })

  it('updates an existing category with an icon chosen from the appearance picker', () => {
    const onUpdate = vi.fn()
    render(<CategoryManagerModal categories={categories} onCreate={vi.fn()} onUpdate={onUpdate} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.click(screen.getAllByRole('button', { name: 'categories.edit' })[0])
    fireEvent.click(screen.getByLabelText('categories.edit_appearance'))
    fireEvent.change(screen.getByLabelText('categories.icon_search_label'), { target: { value: 'circle' } })
    fireEvent.click(screen.getByRole('button', { name: 'Circle' }))
    fireEvent.click(screen.getByText('categories.icon_confirm'))
    fireEvent.click(screen.getByText('form.save'))

    expect(onUpdate).toHaveBeenCalledWith('c1', 'Alimentation', expect.any(String), 'Circle')
  })
})
