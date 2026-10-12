import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { StatsSummary } from './StatsSummary'
import type { FinanceStats } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const stats: FinanceStats = {
  balance: 579.7, balanceAsOf: '2026-01-31', monthNet: 579.7, totalExpense: 420.3, totalIncome: 1000, remainingBudget: 1285.2,
  breakdown: [], budgetVsActual: [],
}

describe('StatsSummary', () => {
  it('renders the formatted balance, spent, income and remaining budget', () => {
    render(<StatsSummary stats={stats} isCurrentMonth={false} />)

    expect(screen.getAllByText(/579,70/).length).toBeGreaterThan(0)
    expect(screen.getByText(/420,30/)).toBeDefined()
    expect(screen.getByText(/1\s*000,00/)).toBeDefined()
    expect(screen.getByText(/1\s*285,20/)).toBeDefined()
  })

  it('renders zero amounts when stats have not loaded yet', () => {
    render(<StatsSummary isCurrentMonth />)

    expect(screen.getAllByText(/0,00/).length).toBe(4)
  })

  it("calls the current month's balance today's, and any other month's its end-of-month one", () => {
    const { rerender } = render(<StatsSummary stats={stats} isCurrentMonth />)
    expect(screen.getByText('stats.balance_today')).toBeDefined()

    rerender(<StatsSummary stats={stats} isCurrentMonth={false} />)
    expect(screen.getByText('stats.balance_end_of_month')).toBeDefined()
  })

  it("shows the month's own result under the balance, signed", () => {
    const { rerender } = render(<StatsSummary stats={stats} isCurrentMonth />)
    expect(screen.getByText(/stats\.month_net:.*\+ 579,70/)).toBeDefined()

    rerender(<StatsSummary stats={{ ...stats, monthNet: -42 }} isCurrentMonth />)
    expect(screen.getByText(/stats\.month_net:.*– 42,00/)).toBeDefined()
  })

  it('offers to adjust the balance only when given the means, outside the card button', () => {
    const onAdjustBalance = vi.fn()
    const { rerender } = render(<StatsSummary stats={stats} isCurrentMonth />)
    expect(screen.queryByRole('button', { name: 'stats.adjust' })).toBeNull()

    rerender(<StatsSummary stats={stats} isCurrentMonth onAdjustBalance={onAdjustBalance} />)
    const adjust = screen.getByRole('button', { name: 'stats.adjust' })
    expect(adjust.parentElement?.closest('button')).toBeNull()
    fireEvent.click(adjust)
    expect(onAdjustBalance).toHaveBeenCalled()
  })

  it('requests the operations list when the balance card is clicked', () => {
    const onSelectOperations = vi.fn()
    render(<StatsSummary stats={stats} isCurrentMonth onSelectOperations={onSelectOperations} />)

    fireEvent.click(screen.getByText('stats.balance_today'))

    expect(onSelectOperations).toHaveBeenCalled()
  })

  it('requests the expense breakdown when the spent card is clicked', () => {
    const onSelectExpenseBreakdown = vi.fn()
    render(<StatsSummary stats={stats} isCurrentMonth onSelectExpenseBreakdown={onSelectExpenseBreakdown} />)

    fireEvent.click(screen.getByText('stats.spent'))

    expect(onSelectExpenseBreakdown).toHaveBeenCalled()
  })

  it('requests the income breakdown when the income card is clicked', () => {
    const onSelectIncomeBreakdown = vi.fn()
    render(<StatsSummary stats={stats} isCurrentMonth onSelectIncomeBreakdown={onSelectIncomeBreakdown} />)

    fireEvent.click(screen.getByText('stats.income'))

    expect(onSelectIncomeBreakdown).toHaveBeenCalled()
  })

  it('does not render the remaining-budget card as a button, since it has no click handler', () => {
    render(<StatsSummary stats={stats} isCurrentMonth />)

    expect(screen.getByText('stats.remaining_budget').closest('button')).toBeNull()
  })
})
