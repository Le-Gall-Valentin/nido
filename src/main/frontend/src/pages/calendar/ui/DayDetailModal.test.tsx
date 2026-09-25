import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { DayDetailModal } from './DayDetailModal'
import type { CalendarOccurrence } from '@/entities/calendar'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, vars?: Record<string, unknown>) =>
      vars ? `${key}:${Object.values(vars).join(',')}` : key,
  }),
}))

function occurrence(overrides: Partial<CalendarOccurrence> & { title: string }): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: overrides.title, seriesId: null, originalDate: null, materialized: true,
    description: null, location: null, allDay: true, startDate: '2026-09-23', startTime: null,
    endDate: '2026-09-23', endTime: null, color: null, participantIds: [], ...overrides,
  }
}

function renderDay(occurrences: CalendarOccurrence[], canWrite = true) {
  const onCreateEvent = vi.fn()
  const onSelectOccurrence = vi.fn()
  render(
    <DayDetailModal date="2026-09-23" occurrences={occurrences} canWrite={canWrite}
      onCreateEvent={onCreateEvent} onSelectOccurrence={onSelectOccurrence} onClose={vi.fn()} />)
  return { onCreateEvent, onSelectOccurrence }
}

describe('DayDetailModal', () => {
  it('names the day in full rather than as an ISO date', () => {
    renderDay([])
    // Titled by the same formatter as the day view's own heading. test-setup pins the language
    // to French, which is what these strings are in.
    expect(screen.getByRole('heading', { level: 2 }).textContent).toBe('Mercredi 23 septembre 2026')
  })

  it('shows each event\'s location and description', () => {
    renderDay([occurrence({ title: 'Concert', location: 'Salle Pleyel', description: 'Apporter les billets' })])
    expect(screen.getByText('Salle Pleyel')).toBeTruthy()
    expect(screen.getByText('Apporter les billets')).toBeTruthy()
  })

  it('shows the time span of a timed event', () => {
    renderDay([occurrence({ title: 'Piano', allDay: false, startTime: '18:00:00', endTime: '19:30:00' })])
    expect(screen.getByText('18:00 – 19:30')).toBeTruthy()
  })

  it('shows the date span of an event running over several days', () => {
    renderDay([occurrence({ title: 'Vacances', startDate: '2026-09-20', endDate: '2026-09-27' })])
    // Same wording as the week heading: the month is written once when both ends share it.
    expect(screen.getByText('20 – 27 septembre 2026')).toBeTruthy()
  })

  it('leaves out the detail lines an item does not have', () => {
    const { container } = render(
      <DayDetailModal date="2026-09-23" occurrences={[occurrence({ title: 'Poubelles', source: 'TASK' })]}
        canWrite onCreateEvent={vi.fn()} onSelectOccurrence={vi.fn()} onClose={vi.fn()} />)
    expect(container.querySelectorAll('[data-testid="occurrence-location"]')).toHaveLength(0)
    expect(container.querySelectorAll('[data-testid="occurrence-description"]')).toHaveLength(0)
  })

  it('offers to add an event on that very day', () => {
    const { onCreateEvent } = renderDay([])
    fireEvent.click(screen.getByRole('button', { name: 'day_detail.add_event' }))
    expect(onCreateEvent).toHaveBeenCalledWith('2026-09-23')
  })

  it('does not offer to add anything to a viewer', () => {
    renderDay([], false)
    expect(screen.queryByRole('button', { name: 'day_detail.add_event' })).toBeNull()
  })

  it('still opens an item when it is tapped', () => {
    const { onSelectOccurrence } = renderDay([occurrence({ title: 'Concert' })])
    fireEvent.click(screen.getByText('Concert'))
    expect(onSelectOccurrence).toHaveBeenCalledOnce()
  })
})
