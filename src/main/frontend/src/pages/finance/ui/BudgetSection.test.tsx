import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BudgetSection } from './BudgetSection'
import type { BudgetLine, Category } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const alimentation: Category = { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true, type: 'EXPENSE' }
const categoryById = new Map([['c1', alimentation]])

describe('BudgetSection', () => {
  it('shows the manage-budget button and reacts to it only for a writer', () => {
    const onManageBudget = vi.fn()
    render(<BudgetSection budgetVsActual={[]} categoryById={categoryById} canWrite onManageBudget={onManageBudget} onSelectCategory={vi.fn()} />)

    fireEvent.click(screen.getByText('budget.manage'))

    expect(onManageBudget).toHaveBeenCalled()
  })

  it('hides the manage-budget button from a read-only viewer', () => {
    render(<BudgetSection budgetVsActual={[]} categoryById={categoryById} canWrite={false} onManageBudget={vi.fn()} onSelectCategory={vi.fn()} />)

    expect(screen.queryByText('budget.manage')).toBeNull()
  })

  it('flags an over-budget line with the overrun amount', () => {
    const lines: BudgetLine[] = [{ categoryId: 'c1', monthlyLimit: 200, spent: 250, status: 'OVER' }]
    render(<BudgetSection budgetVsActual={lines} categoryById={categoryById} canWrite onManageBudget={vi.fn()} onSelectCategory={vi.fn()} />)

    expect(screen.getByText('budget.over:{"amount":"50,00 €"}')).toBeDefined()
  })

  it('flags a category budgeted at exactly 0€ as over budget as soon as the server says so', () => {
    const lines: BudgetLine[] = [{ categoryId: 'c1', monthlyLimit: 0, spent: 10, status: 'OVER' }]
    render(<BudgetSection budgetVsActual={lines} categoryById={categoryById} canWrite onManageBudget={vi.fn()} onSelectCategory={vi.fn()} />)

    expect(screen.getByText('budget.over:{"amount":"10,00 €"}')).toBeDefined()
  })

  it('does not flag a category budgeted at exactly 0€ when nothing has been spent in it yet', () => {
    const lines: BudgetLine[] = [{ categoryId: 'c1', monthlyLimit: 0, spent: 0, status: 'OK' }]
    render(<BudgetSection budgetVsActual={lines} categoryById={categoryById} canWrite onManageBudget={vi.fn()} onSelectCategory={vi.fn()} />)

    expect(screen.getByText('budget.remaining:{"amount":"0,00 €"}')).toBeDefined()
  })

  it('takes the status from the server instead of recomputing it from the amounts', () => {
    // 50 of 200 would be OK by arithmetic: only the server's word can make it a warning here.
    const lines: BudgetLine[] = [{ categoryId: 'c1', monthlyLimit: 200, spent: 50, status: 'WARNING' }]
    render(<BudgetSection budgetVsActual={lines} categoryById={categoryById} canWrite onManageBudget={vi.fn()} onSelectCategory={vi.fn()} />)

    expect(screen.getByTestId('budget-warning-c1')).toBeDefined()
    expect(screen.getByText('budget.remaining:{"amount":"150,00 €"}')).toBeDefined()
  })

  it('shows no warning icon on a line the server calls OK', () => {
    const lines: BudgetLine[] = [{ categoryId: 'c1', monthlyLimit: 200, spent: 190, status: 'OK' }]
    render(<BudgetSection budgetVsActual={lines} categoryById={categoryById} canWrite onManageBudget={vi.fn()} onSelectCategory={vi.fn()} />)

    expect(screen.queryByTestId('budget-warning-c1')).toBeNull()
  })

  it('requests the category transactions when a budget line is clicked', () => {
    const onSelectCategory = vi.fn()
    const lines: BudgetLine[] = [{ categoryId: 'c1', monthlyLimit: 200, spent: 50, status: 'OK' }]
    render(<BudgetSection budgetVsActual={lines} categoryById={categoryById} canWrite onManageBudget={vi.fn()} onSelectCategory={onSelectCategory} />)

    fireEvent.click(screen.getByText('Alimentation'))

    expect(onSelectCategory).toHaveBeenCalledWith('c1')
  })
})
