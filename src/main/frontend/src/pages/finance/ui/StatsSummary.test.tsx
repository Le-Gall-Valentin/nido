import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { StatsSummary } from './StatsSummary'
import type { FinanceStats } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const stats: FinanceStats = {
  balance: 579.7, balanceAsOf: '2026-01-20', monthNet: 579.7, totalExpense: 420.3, totalIncome: 1000, remainingBudget: 1285.2,
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

  it('names the balance after the day the server dates it: a month end, or today', () => {
    // The server dates any month's balance at its end, and the current month's at today on the household's calendar,
    // which the page may not know yet: the label follows the server, so it always says what the figure is.
    const { rerender } = render(<StatsSummary stats={{ ...stats, balanceAsOf: '2026-10-12' }} isCurrentMonth={false} />)
    expect(screen.getByText('stats.balance_today')).toBeDefined()

    rerender(<StatsSummary stats={{ ...stats, balanceAsOf: '2026-02-28' }} isCurrentMonth />)
    expect(screen.getByText('stats.balance_end_of_month')).toBeDefined()
  })

  it('names the balance after the month on screen while the figures load', () => {
    const { rerender } = render(<StatsSummary isCurrentMonth />)
    expect(screen.getByText('stats.balance_today')).toBeDefined()

    rerender(<StatsSummary isCurrentMonth={false} />)
    expect(screen.getByText('stats.balance_end_of_month')).toBeDefined()
  })

  it("shows the month's own result under the balance, signed", () => {
    const { rerender } = render(<StatsSummary stats={stats} isCurrentMonth />)
    expect(screen.getByText(/stats\.month_net:.*\+579,70/)).toBeDefined()

    rerender(<StatsSummary stats={{ ...stats, monthNet: -42 }} isCurrentMonth />)
    expect(screen.getByText(/stats\.month_net:.*-42,00/)).toBeDefined()
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

  it('opens the operations list from the whole balance card, named after its label', () => {
    // The card's button covers the whole card, the strip under the pencil included: the pencil sits on top of it.
    const onSelectOperations = vi.fn()
    render(<StatsSummary stats={stats} isCurrentMonth onSelectOperations={onSelectOperations} onAdjustBalance={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'stats.balance_today' }))

    expect(onSelectOperations).toHaveBeenCalled()
  })

  it('requests the expense breakdown when the spent card is clicked', () => {
    const onSelectExpenseBreakdown = vi.fn()
    render(<StatsSummary stats={stats} isCurrentMonth onSelectExpenseBreakdown={onSelectExpenseBreakdown} />)

    fireEvent.click(screen.getByRole('button', { name: 'stats.spent' }))

    expect(onSelectExpenseBreakdown).toHaveBeenCalled()
  })

  it('requests the income breakdown when the income card is clicked', () => {
    const onSelectIncomeBreakdown = vi.fn()
    render(<StatsSummary stats={stats} isCurrentMonth onSelectIncomeBreakdown={onSelectIncomeBreakdown} />)

    fireEvent.click(screen.getByRole('button', { name: 'stats.income' }))

    expect(onSelectIncomeBreakdown).toHaveBeenCalled()
  })

  it('does not render the remaining-budget card as a button, since it has no click handler', () => {
    render(<StatsSummary stats={stats} isCurrentMonth />)

    expect(screen.getByText('stats.remaining_budget').closest('button')).toBeNull()
  })
})
