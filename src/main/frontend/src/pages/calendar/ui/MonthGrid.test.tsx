import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { DndContext } from '@dnd-kit/core'
import { MonthGrid } from './MonthGrid'

// The whole suite mocks i18n and asserts on keys — translations are verified in the browser,
// not here. Interpolation is kept so labels built from a date stay distinguishable.
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, vars?: Record<string, unknown>) =>
      vars ? `${key}:${Object.values(vars).join(',')}` : key,
  }),
}))
import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'

function occurrence(overrides: Partial<CalendarOccurrence> & { title: string }): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: overrides.title, seriesId: null, originalDate: null,
    materialized: true, allDay: true, startDate: '2026-01-14', startTime: null,
    endDate: '2026-01-14', endTime: null, color: null, participantIds: [], ...overrides,
  }
}

function onDay(title: string, source: CalendarSourceType): CalendarOccurrence {
  return occurrence({ title, source })
}

function renderGrid(occurrences: CalendarOccurrence[], today = '2026-01-15', canWrite = true) {
  const onSelectDay = vi.fn()
  const onSelectOccurrence = vi.fn()
  const view = render(
    <DndContext>
      <MonthGrid date="2026-01-14" occurrences={occurrences} today={today} canWrite={canWrite}
        onSelectDay={onSelectDay} onSelectOccurrence={onSelectOccurrence} />
    </DndContext>)
  return { ...view, onSelectDay, onSelectOccurrence }
}

describe('MonthGrid', () => {
  it('renders 42 day cells, so the grid height never jumps between months', () => {
    const { container } = renderGrid([])
    expect(container.querySelectorAll('[aria-label^="open_day:"]')).toHaveLength(42)
  })

  it('labels each occurrence on the desktop layout', () => {
    renderGrid([onDay('Piano', 'EVENT'), onDay('Gratin', 'MEAL')])
    expect(screen.getByText('Piano')).toBeTruthy()
    expect(screen.getByText('Gratin')).toBeTruthy()
  })

  it('caps a busy day at three labels and counts the rest', () => {
    renderGrid(['A', 'B', 'C', 'D', 'E'].map((title) => onDay(title, 'EVENT')))
    expect(screen.getByText('+2')).toBeTruthy()
    expect(screen.queryByText('D')).toBeNull()
  })

  it('shows one dot per source present on the phone layout, not one per occurrence', () => {
    // Three tasks and one meal on the same day must give two dots, not four.
    const { container } = renderGrid([
      onDay('T1', 'TASK'), onDay('T2', 'TASK'), onDay('T3', 'TASK'), onDay('Gratin', 'MEAL'),
    ])
    expect(container.querySelectorAll('[data-testid="day-dot"]')).toHaveLength(2)
  })

  it('orders the dots the same way on every day', () => {
    const { container } = renderGrid([onDay('Gratin', 'MEAL'), onDay('Piano', 'EVENT')])
    const sources = [...container.querySelectorAll('[data-testid="day-dot"]')]
      .map((dot) => dot.getAttribute('data-source'))
    expect(sources).toEqual(['EVENT', 'MEAL'])
  })

  it('marks the household today, not the browser today', () => {
    renderGrid([], '2026-01-15')
    expect(screen.getByRole('button', { name: 'open_day:2026-01-15' }).getAttribute('data-today')).toBe('true')
    expect(screen.getByRole('button', { name: 'open_day:2026-01-14' }).getAttribute('data-today')).toBeNull()
  })

  it('shows a multi-day occurrence on every day it spans', () => {
    renderGrid([occurrence({
      title: 'Vacances', startDate: '2026-01-05', endDate: '2026-01-07',
    })])
    expect(screen.getAllByText('Vacances')).toHaveLength(3)
  })

  it('calls onSelectDay when a day number is activated', () => {
    const { onSelectDay } = renderGrid([])
    fireEvent.click(screen.getByRole('button', { name: 'open_day:2026-01-20' }))
    expect(onSelectDay).toHaveBeenCalledWith('2026-01-20')
  })

  it('calls onSelectOccurrence, and not onSelectDay, when a chip is activated', () => {
    // The chip sits inside a cell that also opens the day; without stopPropagation both fire.
    const { onSelectDay, onSelectOccurrence } = renderGrid([onDay('Piano', 'EVENT')])
    fireEvent.click(screen.getByText('Piano'))
    expect(onSelectOccurrence).toHaveBeenCalledOnce()
    expect(onSelectDay).not.toHaveBeenCalled()
  })

  it('shows a timed occurrence with its start time', () => {
    renderGrid([occurrence({ title: 'Piano', allDay: false, startTime: '18:00', endTime: '19:00' })])
    expect(screen.getByText('18:00')).toBeTruthy()
  })

  it('attaches a drag handle only to what the calendar may move', () => {
    renderGrid([
      onDay('Piano', 'EVENT'),
      onDay('Loyer', 'FINANCE'),
    ])
    expect(screen.getByText('Piano').closest('button')?.getAttribute('data-draggable')).toBe('true')
    // A finance date is an accounting fact, not a plan to drag around.
    expect(screen.getByText('Loyer').closest('button')?.getAttribute('data-draggable')).toBeNull()
  })

  it('attaches no drag handle at all for a viewer', () => {
    renderGrid([onDay('Piano', 'EVENT')], '2026-01-15', false)
    expect(screen.getByText('Piano').closest('button')?.getAttribute('data-draggable')).toBeNull()
  })
})
