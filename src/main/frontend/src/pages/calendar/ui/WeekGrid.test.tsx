import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { DndContext } from '@dnd-kit/core'
import { WeekGrid } from './WeekGrid'
import { DayAgenda } from './DayAgenda'
import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, vars?: Record<string, unknown>) =>
      vars ? `${key}:${Object.values(vars).join(',')}` : key,
  }),
}))

// Resize handles need a fine pointer: a finger cannot hit a six-pixel strip.
let mockPointerIsFine = true
vi.mock('@/shared/lib', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/lib')>()
  return { ...actual, usePointerIsFine: () => mockPointerIsFine }
})

beforeEach(() => { mockPointerIsFine = true })

function occurrence(overrides: Partial<CalendarOccurrence> & { title: string }): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: overrides.title, seriesId: null, originalDate: null,
    materialized: true, description: null, location: null, allDay: true, startDate: '2026-01-06', startTime: null,
    endDate: '2026-01-06', endTime: null, color: null, participantIds: [], ...overrides,
  }
}

function timed(title: string, startTime: string, endTime: string): CalendarOccurrence {
  return occurrence({ title, allDay: false, startTime, endTime })
}

function allDayFrom(title: string, source: CalendarSourceType): CalendarOccurrence {
  return occurrence({ title, source })
}

function renderWeek(occurrences: CalendarOccurrence[], canWrite = true) {
  return render(
    <DndContext>
      <WeekGrid date="2026-01-08" occurrences={occurrences} today="2026-01-06" canWrite={canWrite}
        onSelectDay={vi.fn()} onSelectOccurrence={vi.fn()} />
    </DndContext>)
}

/** A grid block is positioned on its wrapper, which also holds its resize handles. */
function positionOf(button: HTMLElement): HTMLElement | null {
  return button.closest<HTMLElement>('[style*="top"]')
}

describe('WeekGrid', () => {
  it('renders seven day sections on the phone layout, not an hour grid', () => {
    // A 7-column hour grid at 390px gives each day ~40px: unreadable and untappable.
    const { container } = renderWeek([])
    expect(container.querySelectorAll('[data-testid="week-day-section"]')).toHaveLength(7)
  })

  it('puts every occurrence without a time into the all-day band', () => {
    const { container } = renderWeek([
      allDayFrom('Anniversaire', 'EVENT'),
      allDayFrom('Poubelles', 'TASK'),
      allDayFrom('Gratin', 'MEAL'),
      allDayFrom('Loyer', 'FINANCE'),
      allDayFrom('Vacances', 'SAVINGS'),
    ])
    // One band per day on a desktop; everything here is on the 6th.
    const bands = [...container.querySelectorAll<HTMLElement>('[data-testid="all-day-band"]')]
    expect(bands.flatMap((band) => within(band).queryAllByRole('button'))).toHaveLength(5)
  })

  // Both layouts render at once and are switched with `md:`, so a title appears twice — once in
  // the phone list, once in the desktop hour grid. Only the latter is positioned.
  function positionedBlock(name: RegExp): HTMLElement {
    const block = screen.getAllByRole('button', { name }).map(positionOf).find((el) => el !== null)
    expect(block).toBeTruthy()
    return block as HTMLElement
  }

  it('positions a timed occurrence by its start hour', () => {
    renderWeek([timed('Piano', '18:00', '19:00')])
    const block = positionedBlock(/Piano/)
    expect(block.style.top).toBe('1080px')   // 18h * 60px
    expect(block.style.height).toBe('60px')
  })

  it('gives a zero-length occurrence a visible minimum height', () => {
    // Otherwise it renders as an invisible, unclickable sliver.
    renderWeek([timed('Rappel', '18:00', '18:00')])
    expect(positionedBlock(/Rappel/).style.height).toBe('20px')
  })

  it('keeps a timed occurrence out of the all-day band', () => {
    // Empty bands still render — they are where a timed event is dropped to become all-day.
    const { container } = renderWeek([timed('Piano', '18:00', '19:00')])
    expect(container.querySelectorAll('[data-testid="all-day-band"] button')).toHaveLength(0)
  })

  it('draws the second half of an overnight event from midnight', () => {
    renderWeek([occurrence({ title: 'Soirée', allDay: false, startDate: '2026-01-06', startTime: '22:00',
      endDate: '2026-01-07', endTime: '02:00' })])
    const tops = screen.getAllByRole('button', { name: /Soirée/ })
      .map(positionOf).filter((el) => el !== null).map((el) => el.style.top).sort()
    expect(tops).toEqual(['0px', '1320px'])
  })

  it('puts resize handles on a timed event block, on a mouse', () => {
    renderWeek([timed('Piano', '18:00', '19:00')])
    expect(screen.getAllByTestId('resize-start')).toHaveLength(1)
    expect(screen.getAllByTestId('resize-end')).toHaveLength(1)
  })

  it('puts no resize handle under a finger', () => {
    mockPointerIsFine = false
    renderWeek([timed('Piano', '18:00', '19:00')])
    expect(screen.queryAllByTestId('resize-end')).toHaveLength(0)
  })

  it('puts handles only on the true start and the true end of an overnight event', () => {
    renderWeek([occurrence({ title: 'Soirée', allDay: false, startDate: '2026-01-06', startTime: '22:00',
      endDate: '2026-01-07', endTime: '02:00' })])
    expect(screen.getAllByTestId('resize-start')).toHaveLength(1)
    expect(screen.getAllByTestId('resize-end')).toHaveLength(1)
  })

  it('makes a band item and a grid block draggable, and nothing for a viewer', () => {
    const { unmount } = renderWeek([allDayFrom('Anniversaire', 'EVENT'), timed('Piano', '18:00', '19:00')])
    expect(document.querySelectorAll('[data-draggable]').length).toBeGreaterThanOrEqual(2)
    unmount()
    renderWeek([allDayFrom('Anniversaire', 'EVENT'), timed('Piano', '18:00', '19:00')], false)
    expect(document.querySelectorAll('[data-draggable]')).toHaveLength(0)
  })

  it('never makes finance or savings draggable', () => {
    renderWeek([allDayFrom('Loyer', 'FINANCE'), allDayFrom('Vacances', 'SAVINGS')])
    expect(document.querySelectorAll('[data-draggable]')).toHaveLength(0)
  })
})

describe('DayAgenda', () => {
  function renderDay(occurrences: CalendarOccurrence[]) {
    return render(
      <DayAgenda date="2026-01-06" occurrences={occurrences} onSelectOccurrence={vi.fn()} />)
  }

  it('shows an empty state when the day holds nothing', () => {
    renderDay([])
    expect(screen.getByText('empty_day')).toBeTruthy()
  })

  it('shows the all-day band and the hour grid together', () => {
    const { container } = renderDay([
      allDayFrom('Poubelles', 'TASK'),
      timed('Piano', '18:00', '19:00'),
    ])
    expect(container.querySelector('[data-testid="all-day-band"]')).toBeTruthy()
    expect(positionOf(screen.getByRole('button', { name: /Piano/ }))?.style.top).toBe('1080px')
  })

  it('includes a multi-day occurrence on a day in the middle of its span', () => {
    renderDay([occurrence({ title: 'Vacances', startDate: '2026-01-03', endDate: '2026-01-10' })])
    expect(screen.getByRole('button', { name: /Vacances/ })).toBeTruthy()
  })
})
