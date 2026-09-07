import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { SpaceMember } from '@/entities/space'
import type { RecurringTaskSeries } from '@/entities/tasks'
import { RecurringTaskSeriesFormModal } from './RecurringTaskSeriesFormModal'

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
  anchorDate: '2026-01-07', endDate: null, rotationMemberIds: ['u-1'],
}

describe('RecurringTaskSeriesFormModal', () => {
  it('pre-fills every field from the series', () => {
    render(<RecurringTaskSeriesFormModal series={SERIES} members={MEMBERS} isPersonal={false} onSubmit={vi.fn()} onCancel={vi.fn()} />)

    expect((screen.getByLabelText('form.title_label') as HTMLInputElement).value).toBe('Sortir les poubelles')
    expect(screen.getByText('Vérifier le tri')).toBeDefined()
  })

  it('submits the edited fields', () => {
    const onSubmit = vi.fn()
    render(<RecurringTaskSeriesFormModal series={SERIES} members={MEMBERS} isPersonal={false} onSubmit={onSubmit} onCancel={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Sortir les poubelles et le compost' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Sortir les poubelles et le compost', priority: 'MED', subtasks: ['Vérifier le tri'],
      recurrence: {
        intervalType: 'WEEKLY', intervalCount: 1, leadIntervalType: 'DAILY', leadIntervalCount: 2,
        anchorDate: '2026-01-07', endDate: null, rotationMemberIds: ['u-1'],
      },
    })
  })

  it('rejects a lead time longer than the recurrence interval', () => {
    const onSubmit = vi.fn()
    render(<RecurringTaskSeriesFormModal series={SERIES} members={MEMBERS} isPersonal={false} onSubmit={onSubmit} onCancel={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('recurring_series.lead_time_count_label'), { target: { value: '8' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('form.lead_time_exceeds_interval')).toBeDefined()
  })

  it('rejects an end date before the anchor date', () => {
    const onSubmit = vi.fn()
    render(<RecurringTaskSeriesFormModal series={SERIES} members={MEMBERS} isPersonal={false} onSubmit={onSubmit} onCancel={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('recurring_series.end_date_label'), { target: { value: '2025-01-01' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('recurring_series.end_date_before_start')).toBeDefined()
  })

  it('hides the rotation picker in a personal space', () => {
    render(<RecurringTaskSeriesFormModal series={SERIES} members={[]} isPersonal onSubmit={vi.fn()} onCancel={vi.fn()} />)

    expect(screen.queryByText('form.recurrence_rotation_label')).toBeNull()
  })

  it('shows the submit error from a failed backend call', () => {
    render(<RecurringTaskSeriesFormModal series={SERIES} members={MEMBERS} isPersonal={false}
      onSubmit={vi.fn()} onCancel={vi.fn()} submitError="form.submit_error" />)

    expect(screen.getByText('form.submit_error')).toBeDefined()
  })
})
