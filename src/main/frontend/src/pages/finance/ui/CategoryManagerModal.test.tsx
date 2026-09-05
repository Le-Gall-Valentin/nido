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

  it('creates a new category with an icon chosen from the appearance picker', () => {
    const onCreate = vi.fn()
    render(<CategoryManagerModal categories={categories} onCreate={onCreate} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.change(screen.getByLabelText('categories.new_category_label'), { target: { value: 'Sport' } })
    fireEvent.click(screen.getByLabelText('categories.choose_appearance'))
    fireEvent.change(screen.getByLabelText('categories.icon_search_label'), { target: { value: 'circle' } })
    fireEvent.click(screen.getByRole('button', { name: 'Circle' }))
    fireEvent.click(screen.getByText('categories.icon_confirm'))
    fireEvent.click(screen.getByText('categories.add'))

    expect(onCreate).toHaveBeenCalledWith('Sport', expect.any(String), 'Circle')
  })

  it('creates a new category with a color chosen from the appearance picker', () => {
    const onCreate = vi.fn()
    render(<CategoryManagerModal categories={categories} onCreate={onCreate} onUpdate={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} deleteError={null} />)

    fireEvent.change(screen.getByLabelText('categories.new_category_label'), { target: { value: 'Sport' } })
    fireEvent.click(screen.getByLabelText('categories.choose_appearance'))
    fireEvent.change(screen.getByLabelText('categories.color_label'), { target: { value: '#112233' } })
    fireEvent.click(screen.getByText('categories.icon_confirm'))
    fireEvent.click(screen.getByText('categories.add'))

    expect(onCreate).toHaveBeenCalledWith('Sport', '#112233', expect.any(String))
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
