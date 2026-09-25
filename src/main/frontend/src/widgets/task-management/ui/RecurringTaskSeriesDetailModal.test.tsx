import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { SpaceMember } from '@/entities/space'
import type { RecurringTaskSeries } from '@/entities/tasks'
import { RecurringTaskSeriesDetailModal } from './RecurringTaskSeriesDetailModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const MEMBERS: SpaceMember[] = [
  { userId: 'u-1', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
  { userId: 'u-2', username: 'bob', email: 'b@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
]

const SERIES: RecurringTaskSeries = {
  id: 's-1', title: 'Sortir les poubelles', priority: 'MED', subtaskTemplates: ['Vérifier le tri'],
  intervalType: 'WEEKLY', intervalCount: 1, leadIntervalType: 'DAILY', leadIntervalCount: 2,
  anchorDate: '2026-01-07', endDate: '2027-01-01', rotationMemberIds: ['u-1', 'u-2'], createdBy: 'u-2',
}

describe('RecurringTaskSeriesDetailModal', () => {
  it('shows the title, recurrence details, creator and every rotation participant', () => {
    render(<RecurringTaskSeriesDetailModal series={SERIES} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('Sortir les poubelles')).toBeDefined()
    expect(screen.getByText('2026-01-07')).toBeDefined()
    expect(screen.getByText('2027-01-01')).toBeDefined()
    expect(screen.getByText('alice')).toBeDefined()
    expect(screen.getAllByText('bob').length).toBeGreaterThan(0)
  })

  it('shows every subtask template', () => {
    render(<RecurringTaskSeriesDetailModal series={SERIES} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('Vérifier le tri')).toBeDefined()
  })

  it('shows a fallback creator label when createdBy is null', () => {
    render(<RecurringTaskSeriesDetailModal series={{ ...SERIES, createdBy: null }} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('detail.unknown_creator')).toBeDefined()
  })

  it('shows a fallback when the series has no rotation members', () => {
    render(<RecurringTaskSeriesDetailModal series={{ ...SERIES, rotationMemberIds: [] }} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.getByText('detail.no_assignee')).toBeDefined()
  })

  it('does not show an end date row when the series has none', () => {
    render(<RecurringTaskSeriesDetailModal series={{ ...SERIES, endDate: null }} members={MEMBERS} onClose={vi.fn()} />)

    expect(screen.queryByText('recurring_series.end_date_label')).toBeNull()
  })
})
