import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { CategoryManagerModal } from './CategoryManagerModal'
import type { Category } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const categories: Category[] = [
  { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true },
  { id: 'c2', label: 'Animaux', color: '#a3e635', icon: 'PawPrint', isDefault: false },
]

describe('CategoryManagerModal', () => {
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

    expect(onCreate).toHaveBeenCalledWith('Sport', expect.any(String), expect.any(String))
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
})
