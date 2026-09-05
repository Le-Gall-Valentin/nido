import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { SavingsGoalContributionsModal } from './SavingsGoalContributionsModal'
import type { SavingsContribution } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

function memberLabel(memberId: string): string {
  return memberId === 'alice' ? 'alice' : 'bob'
}

const contributions: SavingsContribution[] = [
  { id: 'c1', memberId: 'alice', amount: 100, date: '2026-01-05' },
  { id: 'c2', memberId: 'bob', amount: 50, date: '2026-02-01' },
]

describe('SavingsGoalContributionsModal', () => {
  it('lists every contribution with who, when and how much', () => {
    render(
      <SavingsGoalContributionsModal
        goalName="Vacances" color="#4a7fa0" glyph="🏠" contributions={contributions} memberLabel={memberLabel} onClose={vi.fn()}
      />
    )

    expect(screen.getByText('Vacances')).toBeDefined()
    expect(screen.getByText('🏠')).toBeDefined()
    expect(screen.getByText('alice')).toBeDefined()
    expect(screen.getByText('2026-01-05')).toBeDefined()
    expect(screen.getByText(/100,00/)).toBeDefined()
    expect(screen.getByText('bob')).toBeDefined()
    expect(screen.getByText('2026-02-01')).toBeDefined()
    expect(screen.getByText(/50,00/)).toBeDefined()
  })

  it('shows an empty state when the goal has no contributions yet', () => {
    render(
      <SavingsGoalContributionsModal
        goalName="Vacances" color="#4a7fa0" glyph="🏠" contributions={[]} memberLabel={memberLabel} onClose={vi.fn()}
      />
    )

    expect(screen.getByText('savings.contributions_empty')).toBeDefined()
  })
})
