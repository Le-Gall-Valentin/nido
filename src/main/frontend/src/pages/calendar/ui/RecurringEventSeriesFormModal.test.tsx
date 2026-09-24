import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { RecurringEventSeriesFormModal } from './RecurringEventSeriesFormModal'
import type { RecurringEventSeries } from '@/entities/calendar'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

const members: SpaceMember[] = [
  { userId: 'u-1', username: 'alice', email: null, role: 'OWNER', joinedAt: '2026-01-01T00:00:00Z' },
  { userId: 'u-2', username: 'bob', email: null, role: 'MEMBER', joinedAt: '2026-01-01T00:00:00Z' },
]

function renderForm(options: { series?: RecurringEventSeries | null; isPersonal?: boolean; defaultDate?: string } = {}) {
  const onSubmit = vi.fn()
  render(
    <RecurringEventSeriesFormModal series={options.series ?? null} members={members} currentUserId="u-1"
      isPersonal={options.isPersonal ?? false} defaultDate={options.defaultDate ?? '2026-09-24'}
      onSubmit={onSubmit} onCancel={vi.fn()} />)
  return { onSubmit }
}

const pressed = (name: string) => screen.getByRole('button', { name }).getAttribute('aria-pressed')

describe('RecurringEventSeriesFormModal — participants', () => {
  it('lets a shared context pick who takes part, the creator ticked by default', () => {
    const { onSubmit } = renderForm()
    expect(pressed('alice')).toBe('true')
    expect(pressed('bob')).toBe('false')

    fireEvent.click(screen.getByRole('button', { name: 'bob' }))
    fireEvent.change(screen.getByLabelText('form.title'), { target: { value: 'Piano' } })
    fireEvent.change(screen.getByLabelText('series.anchor_date'), { target: { value: '2026-10-07' } })
    fireEvent.click(screen.getByRole('button', { name: 'form.save' }))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ participantIds: ['u-1', 'u-2'] }))
  })

  it('offers no choice in a personal context', () => {
    renderForm({ isPersonal: true })
    expect(screen.queryByText('form.participants')).toBeNull()
    expect(screen.queryByRole('button', { name: 'alice' })).toBeNull()
  })

  it('keeps the participants of a series being edited rather than the creator default', () => {
    renderForm({ defaultDate: '2026-12-01', series: {
      id: 's-1', title: 'Piano', description: null, location: null, allDay: true, startTime: null, endTime: null,
      durationDays: 0, color: null, intervalType: 'WEEKLY', intervalCount: 1, anchorDate: '2026-10-07', endDate: null,
      participantIds: ['u-2'], createdBy: 'u-2', createdAt: '2026-01-01T00:00:00Z',
    } as RecurringEventSeries })
    expect(pressed('alice')).toBe('false')
    expect(pressed('bob')).toBe('true')
    expect(anchorDate()).toBe('2026-10-07')
  })
})

const anchorDate = () => (screen.getByLabelText('series.anchor_date') as HTMLInputElement).value

describe('RecurringEventSeriesFormModal — start date', () => {
  it('starts a new series on the day shown, so it saves without a date being typed', () => {
    const { onSubmit } = renderForm({ defaultDate: '2026-10-07' })
    expect(anchorDate()).toBe('2026-10-07')

    fireEvent.change(screen.getByLabelText('form.title'), { target: { value: 'Piano' } })
    fireEvent.click(screen.getByRole('button', { name: 'form.save' }))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ anchorDate: '2026-10-07' }))
  })
})
