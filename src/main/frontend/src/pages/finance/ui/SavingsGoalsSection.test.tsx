import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SavingsGoalsSection } from './SavingsGoalsSection'
import type { SavingsGoal } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

function memberLabel(memberId: string): string {
  return memberId === 'alice' ? 'alice' : 'bob'
}

const vacances: SavingsGoal = {
  id: 'g1', name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯',
  totalContributed: 500, contributions: [{ id: 'c1', memberId: 'alice', amount: 500, date: '2026-01-05' }],
}

describe('SavingsGoalsSection', () => {
  it('shows an empty state when there are no goals', () => {
    render(
      <SavingsGoalsSection savingsGoals={[]} canWrite memberLabel={memberLabel}
        onCreate={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} onContribute={vi.fn()} onView={vi.fn()} />
    )

    expect(screen.getByText('savings.empty')).toBeDefined()
  })

  it('opens the contribution detail when the card itself is clicked', () => {
    const onView = vi.fn()
    render(
      <SavingsGoalsSection savingsGoals={[vacances]} canWrite memberLabel={memberLabel}
        onCreate={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} onContribute={vi.fn()} onView={onView} />
    )

    fireEvent.click(screen.getByText('Vacances'))

    expect(onView).toHaveBeenCalledWith(vacances)
  })

  it('edits and deletes the goal without also opening the contribution detail', () => {
    const onEdit = vi.fn()
    const onDelete = vi.fn()
    const onView = vi.fn()
    render(
      <SavingsGoalsSection savingsGoals={[vacances]} canWrite memberLabel={memberLabel}
        onCreate={vi.fn()} onEdit={onEdit} onDelete={onDelete} onContribute={vi.fn()} onView={onView} />
    )

    fireEvent.click(screen.getByLabelText('savings.edit'))
    fireEvent.click(screen.getByLabelText('savings.delete'))

    expect(onEdit).toHaveBeenCalledWith(vacances)
    expect(onDelete).toHaveBeenCalledWith('g1')
    expect(onView).not.toHaveBeenCalled()
  })

  it('hides the contribute button and shows the done badge once the goal is fully funded', () => {
    const done: SavingsGoal = { ...vacances, totalContributed: 2000 }
    render(
      <SavingsGoalsSection savingsGoals={[done]} canWrite memberLabel={memberLabel}
        onCreate={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} onContribute={vi.fn()} onView={vi.fn()} />
    )

    expect(screen.getByText('savings.done')).toBeDefined()
    expect(screen.queryByText('savings.contribute')).toBeNull()
  })

  it('hides the new-goal button and every per-goal action from a read-only viewer', () => {
    render(
      <SavingsGoalsSection savingsGoals={[vacances]} canWrite={false} memberLabel={memberLabel}
        onCreate={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} onContribute={vi.fn()} onView={vi.fn()} />
    )

    expect(screen.queryByText('savings.new_goal')).toBeNull()
    expect(screen.queryByLabelText('savings.edit')).toBeNull()
    expect(screen.queryByLabelText('savings.delete')).toBeNull()
    expect(screen.queryByText('savings.contribute')).toBeNull()
  })
})
