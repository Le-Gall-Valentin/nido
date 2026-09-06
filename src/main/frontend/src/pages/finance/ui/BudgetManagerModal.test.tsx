import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import type { Category, BudgetLine } from '@/entities/finance'
import { BudgetManagerModal } from './BudgetManagerModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const CATEGORIES: Category[] = [
  { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true },
  { id: 'c2', label: 'Loisirs', color: '#3b82f6', icon: 'Gamepad2', isDefault: false },
]

const BUDGET_LINES: BudgetLine[] = [{ categoryId: 'c1', monthlyLimit: 300, spent: 120 }]

describe('BudgetManagerModal', () => {
  it('shows the submit error handed down by the caller when the backend rejected a save', () => {
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={vi.fn()} onClose={vi.fn()} submitError="form.submit_error" />)

    expect(screen.getByText('form.submit_error')).toBeDefined()
  })

  it('lists every category with its current budget, or the option to set one', () => {
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('Alimentation')).toBeDefined()
    expect(screen.getByText('Loisirs')).toBeDefined()
    expect(screen.getByText('budget.edit')).toBeDefined()
    expect(screen.getByText('budget.set')).toBeDefined()
  })

  it('edits the budget of a category that already has one', () => {
    const onSave = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={onSave} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('budget.edit'))
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '350' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSave).toHaveBeenCalledWith('c1', 350)
  })

  it('sets a budget for a category that does not have one yet', () => {
    const onSave = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={onSave} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('budget.set'))
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '150' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSave).toHaveBeenCalledWith('c2', 150)
  })

  it('does not save a blank budget value', () => {
    const onSave = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={onSave} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('budget.set'))
    fireEvent.click(screen.getByText('form.save'))

    expect(onSave).not.toHaveBeenCalled()
  })
})
