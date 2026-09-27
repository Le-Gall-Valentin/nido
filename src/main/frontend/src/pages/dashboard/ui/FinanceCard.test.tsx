import { fireEvent, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { formatAmount } from '@/shared/lib'
import type { FinanceCard as FinanceCardData } from '@/entities/dashboard'
import { FinanceCard } from './FinanceCard'
import { renderWithActions } from '../test/renderWithActions'

/** Testing Library normalizes the DOM's no-break spaces, not an exact matcher: normalize it the same way. */
const plain = (text: string) => text.replace(/\s+/g, ' ')

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const CARD: FinanceCardData = {
  month: '2026-09', balance: 1240, totalExpense: 1860, totalIncome: 3100, remainingBudget: 340,
  budgetsToWatch: [
    { categoryId: 'c-resto', label: 'Restaurants', color: null, spent: 212, limit: 180, status: 'OVER' },
    { categoryId: 'c-food', label: 'Courses alimentaires', color: null, spent: 430, limit: 500, status: 'WARNING' },
  ],
  upcoming: [
    { date: '2026-09-28', label: 'Netflix', amount: 13.49, type: 'EXPENSE', seriesId: 's-1' },
    { date: '2026-10-01', label: 'Salaire', amount: 2100, type: 'INCOME', seriesId: 's-2' },
  ],
  balances: [
    { memberId: 'u-cam', amount: 42.5, direction: 'I_OWE' },
    { memberId: 'u-paul', amount: 18, direction: 'OWES_ME' },
  ],
}

describe('FinanceCard', () => {
  it('shows the month\'s four figures, the balance signed', () => {
    renderWithActions(<FinanceCard card={CARD} />)

    expect(screen.getByText('Septembre')).toBeDefined()
    expect(screen.getByText(plain(`+${formatAmount(1240)}`))).toBeDefined()
    expect(screen.getByText(plain(formatAmount(1860)))).toBeDefined()
    expect(screen.getByText(plain(formatAmount(3100)))).toBeDefined()
    expect(screen.getByText(plain(formatAmount(340)))).toBeDefined()
  })

  it('draws an overrun budget in red and a nearly spent one in orange', () => {
    renderWithActions(<FinanceCard card={CARD} />)

    expect(screen.getByTestId('budget-bar-c-resto').className).toContain('bg-status-red')
    expect(screen.getByTestId('budget-bar-c-resto').style.width).toBe('100%')
    expect(screen.getByTestId('budget-bar-c-food').className).toContain('bg-status-orange')
    expect(screen.getByTestId('budget-bar-c-food').style.width).toBe('86%')
  })

  it('fills the bar of a 0 € budget instead of dividing by zero', () => {
    renderWithActions(<FinanceCard card={{ ...CARD, budgetsToWatch: [{ categoryId: 'c-zero', label: 'Tabac', color: null, spent: 5, limit: 0, status: 'OVER' }] }} />)

    expect(screen.getByTestId('budget-bar-c-zero').style.width).toBe('100%')
  })

  it('signs what comes in and what goes out', () => {
    renderWithActions(<FinanceCard card={CARD} />)

    expect(screen.getByText(plain(`−${formatAmount(13.49)}`))).toBeDefined()
    expect(screen.getByText(plain(`+${formatAmount(2100)}`)).className).toContain('text-status-green')
  })

  it('lets me settle what I owe, and only that', () => {
    const settle = vi.fn()
    renderWithActions(<FinanceCard card={CARD} />, { actions: { settle } })

    expect(screen.getByText(plain(`finance.owes_me:${JSON.stringify({ name: 'paul', amount: formatAmount(18) })}`))).toBeDefined()
    const buttons = screen.getAllByRole('button', { name: 'finance.settle' })
    expect(buttons).toHaveLength(1)
    fireEvent.click(buttons[0])
    expect(settle).toHaveBeenCalledWith({ toMemberId: 'u-cam', amount: 42.5 })
  })

  it('has no "between you" in a personal space and no settle button for a viewer', () => {
    renderWithActions(<FinanceCard card={{ ...CARD, balances: null }} />, { actions: { canWrite: false } })

    expect(screen.queryByText('finance.balances')).toBeNull()
    expect(screen.queryByRole('button', { name: 'finance.settle' })).toBeNull()
  })
})
