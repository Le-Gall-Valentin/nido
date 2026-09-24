import { describe, it, expect, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { WeekGrid } from './WeekGrid'
import { DayAgenda } from './DayAgenda'
import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, vars?: Record<string, unknown>) =>
      vars ? `${key}:${Object.values(vars).join(',')}` : key,
  }),
}))

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

function renderWeek(occurrences: CalendarOccurrence[]) {
  return render(
    <WeekGrid date="2026-01-08" occurrences={occurrences} today="2026-01-06"
      onSelectDay={vi.fn()} onSelectOccurrence={vi.fn()} />)
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
    const band = container.querySelector('[data-testid="all-day-band"]')
    expect(band).toBeTruthy()
    expect(within(band as HTMLElement).getAllByRole('button')).toHaveLength(5)
  })

  // Both layouts render at once and are switched with `md:`, so a title appears twice — once in
  // the phone list, once in the desktop hour grid. Only the latter is positioned.
  function positionedBlock(name: RegExp): HTMLElement {
    const block = screen.getAllByRole('button', { name }).find((el) => el.style.top !== '')
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
    const { container } = renderWeek([timed('Piano', '18:00', '19:00')])
    expect(container.querySelector('[data-testid="all-day-band"]')).toBeNull()
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
    expect(screen.getByRole('button', { name: /Piano/ }).style.top).toBe('1080px')
  })

  it('includes a multi-day occurrence on a day in the middle of its span', () => {
    renderDay([occurrence({ title: 'Vacances', startDate: '2026-01-03', endDate: '2026-01-10' })])
    expect(screen.getByRole('button', { name: /Vacances/ })).toBeTruthy()
  })
})
