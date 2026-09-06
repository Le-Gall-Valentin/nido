import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import type { Category, BudgetLine } from '@/entities/finance'
import { BudgetManagerModal } from './BudgetManagerModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const CATEGORIES: Category[] = [
  { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true, type: 'EXPENSE' },
  { id: 'c2', label: 'Loisirs', color: '#3b82f6', icon: 'Gamepad2', isDefault: false, type: 'EXPENSE' },
  { id: 'c3', label: 'Revenu', color: '#22c55e', icon: 'Wallet', isDefault: true, type: 'INCOME' },
]

const BUDGET_LINES: BudgetLine[] = [{ categoryId: 'c1', monthlyLimit: 300, spent: 120 }]

describe('BudgetManagerModal', () => {
  it('shows the submit error handed down by the caller when the backend rejected a save', () => {
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} submitError="form.submit_error" />)

    expect(screen.getByText('form.submit_error')).toBeDefined()
  })

  it('lists every category with its current budget, or the option to set one', () => {
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('Alimentation')).toBeDefined()
    expect(screen.getByText('Loisirs')).toBeDefined()
    expect(screen.getByLabelText('budget.edit')).toBeDefined()
    expect(screen.getByLabelText('budget.remove')).toBeDefined()
    expect(screen.getByText('budget.set')).toBeDefined()
  })

  it('never lists an income category, even one with a budget line', () => {
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} />)

    expect(screen.queryByText('Revenu')).toBeNull()
  })

  it('removes the budget of a category that already has one, without asking to confirm', () => {
    const onDelete = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={vi.fn()} onDelete={onDelete} onClose={vi.fn()} />)

    fireEvent.click(screen.getByLabelText('budget.remove'))

    expect(onDelete).toHaveBeenCalledWith('c1')
  })

  it('edits the budget of a category that already has one', () => {
    const onSave = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={onSave} onDelete={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(screen.getByLabelText('budget.edit'))
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '350' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSave).toHaveBeenCalledWith('c1', 350)
  })

  it('sets a budget for a category that does not have one yet', () => {
    const onSave = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={onSave} onDelete={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('budget.set'))
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '150' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSave).toHaveBeenCalledWith('c2', 150)
  })

  it('does not save a blank budget value, and shows an error instead of closing silently', () => {
    const onSave = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={onSave} onDelete={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('budget.set'))
    fireEvent.click(screen.getByText('form.save'))

    expect(onSave).not.toHaveBeenCalled()
    expect(screen.getByText('form.amount_required')).toBeDefined()
    expect(screen.getByRole('spinbutton')).toBeDefined()
  })

  it('does not save a negative budget value, and shows an error instead of closing silently', () => {
    const onSave = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={onSave} onDelete={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(screen.getByLabelText('budget.edit'))
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '-10' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSave).not.toHaveBeenCalled()
    expect(screen.getByText('form.amount_required')).toBeDefined()
  })

  it('clears the error once a valid value is saved', () => {
    const onSave = vi.fn()
    render(<BudgetManagerModal categories={CATEGORIES} budgetLines={BUDGET_LINES} onSave={onSave} onDelete={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('budget.set'))
    fireEvent.click(screen.getByText('form.save'))
    expect(screen.getByText('form.amount_required')).toBeDefined()

    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '150' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSave).toHaveBeenCalledWith('c2', 150)
    expect(screen.queryByText('form.amount_required')).toBeNull()
  })
})
