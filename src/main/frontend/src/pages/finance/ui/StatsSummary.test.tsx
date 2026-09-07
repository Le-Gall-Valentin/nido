import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { StatsSummary } from './StatsSummary'
import type { FinanceStats } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const stats: FinanceStats = {
  balance: 579.7, totalExpense: 420.3, totalIncome: 1000, remainingBudget: 1285.2, breakdown: [], budgetVsActual: [],
}

describe('StatsSummary', () => {
  it('renders the formatted balance, spent, income and remaining budget', () => {
    render(<StatsSummary stats={stats} />)

    expect(screen.getByText(/579,70/)).toBeDefined()
    expect(screen.getByText(/420,30/)).toBeDefined()
    expect(screen.getByText(/1\s*000,00/)).toBeDefined()
    expect(screen.getByText(/1\s*285,20/)).toBeDefined()
  })

  it('renders zero amounts when stats have not loaded yet', () => {
    render(<StatsSummary />)

    expect(screen.getAllByText(/0,00/).length).toBe(4)
  })

  it('requests the operations list when the balance card is clicked', () => {
    const onSelectOperations = vi.fn()
    render(<StatsSummary stats={stats} onSelectOperations={onSelectOperations} />)

    fireEvent.click(screen.getByText('stats.balance'))

    expect(onSelectOperations).toHaveBeenCalled()
  })

  it('requests the expense breakdown when the spent card is clicked', () => {
    const onSelectExpenseBreakdown = vi.fn()
    render(<StatsSummary stats={stats} onSelectExpenseBreakdown={onSelectExpenseBreakdown} />)

    fireEvent.click(screen.getByText('stats.spent'))

    expect(onSelectExpenseBreakdown).toHaveBeenCalled()
  })

  it('requests the income breakdown when the income card is clicked', () => {
    const onSelectIncomeBreakdown = vi.fn()
    render(<StatsSummary stats={stats} onSelectIncomeBreakdown={onSelectIncomeBreakdown} />)

    fireEvent.click(screen.getByText('stats.income'))

    expect(onSelectIncomeBreakdown).toHaveBeenCalled()
  })

  it('does not render the remaining-budget card as a button, since it has no click handler', () => {
    render(<StatsSummary stats={stats} />)

    expect(screen.getByText('stats.remaining_budget').closest('button')).toBeNull()
  })
})
