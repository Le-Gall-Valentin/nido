import { screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { formatAmount } from '@/shared/lib'
import type { SavingsGoalItem } from '@/entities/dashboard'
import { SavingsCard } from './SavingsCard'
import { renderWithActions } from './cardTestHarness'

/** Testing Library normalizes the DOM's no-break spaces, not an exact matcher: normalize it the same way. */
const plain = (text: string) => text.replace(/\s+/g, ' ')

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

function goal(goalId: string, extra: Partial<SavingsGoalItem>): SavingsGoalItem {
  return { goalId, name: goalId, glyph: '🏖️', color: '#cfe0f5', target: 3000, contributed: 1450, targetDate: '2027-06-30', state: 'IN_PROGRESS', monthlyNeeded: 172.23, ...extra }
}

describe('SavingsCard', () => {
  it('says how much is left and how much a month it takes', () => {
    renderWithActions(<SavingsCard card={{ goals: [goal('Vacances', {})] }} />)

    expect(screen.getByText(plain(`${formatAmount(1450)} / ${formatAmount(3000)}`))).toBeDefined()
    expect(screen.getByText(plain(`savings.monthly_needed:${JSON.stringify({ remaining: formatAmount(1550), monthly: formatAmount(172.23), date: 'juin 2027' })}`))).toBeDefined()
  })

  it('celebrates a reached goal and warns about a close or passed date', () => {
    renderWithActions(<SavingsCard card={{ goals: [
      goal('Canapé', { state: 'REACHED', contributed: 900, target: 900, monthlyNeeded: null }),
      goal('Vélo', { state: 'DUE_SOON', targetDate: '2026-10-10', monthlyNeeded: 400 }),
      goal('Écran', { state: 'PAST_DUE', targetDate: '2026-09-01', monthlyNeeded: null }),
    ] }} />)

    expect(screen.getByText('savings.reached').className).toContain('text-status-green')
    expect(screen.getByText('savings.due_soon:{"date":"10 oct."}').className).toContain('text-status-orange')
    expect(screen.getByText('savings.past_due:{"date":"1 sept."}').className).toContain('text-status-orange')
  })

  it('says a goal without a date has none', () => {
    renderWithActions(<SavingsCard card={{ goals: [goal('Fonds', { targetDate: null, monthlyNeeded: null })] }} />)
    expect(screen.getByText('savings.no_target_date')).toBeDefined()
  })
})
