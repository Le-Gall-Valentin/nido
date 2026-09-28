import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { DndContext } from '@dnd-kit/core'
import { MonthGrid } from './MonthGrid'
import { DragPreviewProvider } from '../model/DragPreviewProvider'
import type { DragPreviewState } from '../model/dragPreview'
import { formatDay } from '../lib/periodLabel'

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
    materialized: true, description: null, location: null, allDay: true, startDate: '2026-01-14', startTime: null,
    endDate: '2026-01-14', endTime: null, color: null, participantIds: [], ...overrides,
  }
}

function onDay(title: string, source: CalendarSourceType): CalendarOccurrence {
  return occurrence({ title, source })
}

function renderGrid(occurrences: CalendarOccurrence[], today = '2026-01-15', canWrite = true,
  drag: DragPreviewState | null = null) {
  const onSelectDay = vi.fn()
  const onSelectMonth = vi.fn()
  const onSelectOccurrence = vi.fn()
  const view = render(
    <DndContext>
      <DragPreviewProvider value={drag}>
        <MonthGrid date="2026-01-14" occurrences={occurrences} today={today} canWrite={canWrite}
          onSelectDay={onSelectDay} onSelectMonth={onSelectMonth} onSelectOccurrence={onSelectOccurrence} />
      </DragPreviewProvider>
    </DndContext>)
  return { ...view, onSelectDay, onSelectMonth, onSelectOccurrence }
}

/** A day's button, found by what a screen reader says of it. */
function dayButton(inWords: string) {
  return screen.getByRole('button', { name: `open_day:${inWords}` })
}

/** The button of a day outside the month shown — January 2026 runs from Dec 29 to Feb 8 here. */
function monthButton(inWords: string) {
  return screen.getByRole('button', { name: `show_month:${inWords}` })
}

describe('MonthGrid', () => {
  it('renders 42 day cells, so the grid height never jumps between months', () => {
    const { container } = renderGrid([])
    expect(container.querySelectorAll('[aria-label^="open_day:"], [aria-label^="show_month:"]')).toHaveLength(42)
  })

  it('opens the month of a day outside the one shown, rather than that day', () => {
    const { onSelectDay, onSelectMonth } = renderGrid([])
    fireEvent.click(monthButton('mardi 3 février 2026'))
    expect(onSelectMonth).toHaveBeenCalledWith('2026-02-03')
    expect(onSelectDay).not.toHaveBeenCalled()
  })

  it('opens the month, not the day, from the count of a busy day outside the month shown', () => {
    const busy = ['A', 'B', 'C', 'D', 'E'].map((title) => occurrence({ title, startDate: '2026-02-03', endDate: '2026-02-03' }))
    const { onSelectDay, onSelectMonth } = renderGrid(busy)
    fireEvent.click(screen.getByText('+2'))
    expect(onSelectMonth).toHaveBeenCalledWith('2026-02-03')
    expect(onSelectDay).not.toHaveBeenCalled()
  })

  it('still opens a day of the month shown, and never its month', () => {
    const { onSelectDay, onSelectMonth } = renderGrid([])
    fireEvent.click(dayButton('samedi 31 janvier 2026'))
    expect(onSelectDay).toHaveBeenCalledWith('2026-01-31')
    expect(onSelectMonth).not.toHaveBeenCalled()
  })

  it('names the month on the first of each month, and only there', () => {
    renderGrid([])
    expect(screen.getByText('1 janv.')).toBeTruthy()
    expect(screen.getByText('1 févr.')).toBeTruthy()
    expect(screen.queryByText('2 févr.')).toBeNull()
  })

  // jsdom has no stylesheet: these pin the classes, and the browser shows what they look like.
  it('sets the days outside the month back: a recessed cell, a paler number, faded items', () => {
    renderGrid([
      occurrence({ title: 'Dehors', startDate: '2026-02-02', endDate: '2026-02-02' }),
      occurrence({ title: 'Dedans', startDate: '2026-01-30', endDate: '2026-01-30' }),
    ])
    const outside = monthButton('lundi 2 février 2026')
    const inside = dayButton('vendredi 30 janvier 2026')
    expect(outside.parentElement?.className).toContain('bg-bg-0')
    expect(inside.parentElement?.className).not.toContain('bg-bg-0')
    expect(outside.className).toContain('text-fg-3')
    expect(inside.className).not.toContain('text-fg-3')
    expect(screen.getByText('Dehors').closest('button')?.className).toContain('opacity-50')
    expect(screen.getByText('Dedans').closest('button')?.className).not.toContain('opacity-50')
  })

  it('lets a tap on the faded dots reach the day behind them', () => {
    // Faded, the row of dots is drawn over the stretched day button: a finger landing on a dot hit
    // the dot, and the day never turned to its month — measured in the browser.
    renderGrid([occurrence({ title: 'Dehors', startDate: '2026-02-02', endDate: '2026-02-02' })])
    const dots = monthButton('lundi 2 février 2026').parentElement?.querySelector('[data-testid="day-dot"]')?.parentElement
    expect(dots?.className).toContain('pointer-events-none')
  })

  it('names each day in words for a screen reader, not as a date code', () => {
    renderGrid([])
    expect(dayButton('jeudi 15 janvier 2026')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'open_day:2026-01-15' })).toBeNull()
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
    expect(dayButton('jeudi 15 janvier 2026').getAttribute('data-today')).toBe('true')
    expect(dayButton('mercredi 14 janvier 2026').getAttribute('data-today')).toBeNull()
  })

  it('shows a multi-day occurrence on every day it spans', () => {
    renderGrid([occurrence({
      title: 'Vacances', startDate: '2026-01-05', endDate: '2026-01-07',
    })])
    expect(screen.getAllByText('Vacances')).toHaveLength(3)
  })

  // jsdom has no layout, so these two pin the structure that produces the hit area rather than the
  // hit area itself — which is measured in the browser. Only the 24px day number used to open the
  // day: 4% of a desktop cell, which is why clicking a day on a PC seemed to do nothing.
  it('stretches the day button over its whole cell, so any empty spot opens the day', () => {
    renderGrid([])
    const button = dayButton('mardi 20 janvier 2026')
    expect(button.className).toContain('after:absolute')
    expect(button.className).toContain('after:inset-0')
    expect(button.parentElement?.className).toContain('relative')
  })

  it('keeps chips above the stretched area, so a chip still opens its own event', () => {
    renderGrid(['A', 'B', 'C', 'D'].map((title) => onDay(title, 'EVENT')))
    expect(screen.getByText('A').closest('button')?.className).toContain('z-10')
    expect(screen.getByText('+1').className).toContain('z-10')
  })

  it('calls onSelectDay when a day number is activated', () => {
    const { onSelectDay } = renderGrid([])
    fireEvent.click(dayButton('mardi 20 janvier 2026'))
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
    renderGrid([occurrence({ title: 'Piano', description: null, location: null, allDay: false, startTime: '18:00', endTime: '19:00' })])
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

  it('never blocks a finger that starts a scroll on a chip — a touch tablet shows this grid', () => {
    // The drag is a long press (TouchSensor), which blocks the scroll itself once it starts.
    renderGrid([onDay('Piano', 'EVENT')])
    const chip = screen.getByText('Piano').closest('button') as HTMLElement
    expect(chip.className).not.toContain('touch-none')
    expect(chip.className).toContain('touch-manipulation')
  })

  it('draws a moved trip on every day of its landing span, and dims it where it was', () => {
    const trip = occurrence({ title: 'Voyage', startDate: '2026-01-12', endDate: '2026-01-14' })
    renderGrid([trip], '2026-01-15', true, {
      intent: { kind: 'move', occurrence: trip, from: 'cell', day: '2026-01-13' },
      change: { allDay: true, startDate: '2026-01-19', startTime: null, endDate: '2026-01-21', endTime: null },
    })
    const cellOf = (day: string) => dayButton(formatDay(day, 'fr-FR')).parentElement as HTMLElement
    for (const day of ['2026-01-19', '2026-01-20', '2026-01-21']) {
      expect(cellOf(day).querySelector('[data-testid="drag-preview"]')?.textContent).toContain('Voyage')
    }
    expect(cellOf('2026-01-22').querySelector('[data-testid="drag-preview"]')).toBeNull()
    expect(cellOf('2026-01-13').querySelector('[data-testid="drag-preview"]')).toBeNull()
    expect((cellOf('2026-01-13').querySelector('button[data-draggable]') as HTMLElement).className).toContain('opacity-40')
  })

  it('attaches no drag handle at all for a viewer', () => {
    renderGrid([onDay('Piano', 'EVENT')], '2026-01-15', false)
    expect(screen.getByText('Piano').closest('button')?.getAttribute('data-draggable')).toBeNull()
  })
})
