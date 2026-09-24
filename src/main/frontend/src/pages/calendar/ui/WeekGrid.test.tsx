import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { DndContext } from '@dnd-kit/core'
import { WeekGrid } from './WeekGrid'
import { DayAgenda } from './DayAgenda'
import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'
import { DragPreviewProvider } from '../model/DragPreviewProvider'
import type { DragPreviewState } from '../model/dragPreview'

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

function renderWeek(occurrences: CalendarOccurrence[], canWrite = true, drag: DragPreviewState | null = null) {
  return render(
    <DndContext>
      <DragPreviewProvider value={drag}>
        <WeekGrid date="2026-01-08" occurrences={occurrences} today="2026-01-06" canWrite={canWrite}
          onSelectDay={vi.fn()} onSelectOccurrence={vi.fn()} />
      </DragPreviewProvider>
    </DndContext>)
}

/** The seven hour columns of the desktop grid, Monday first. */
function columnsOf(container: HTMLElement): HTMLElement[] {
  return [...container.querySelectorAll<HTMLElement>('[style*="height: 1440px"]')]
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

  it('keeps resize handles out of the tab order and away from screen readers', () => {
    // Two nameless "button" stops per event otherwise; the edit form is how they change times.
    renderWeek([timed('Piano', '18:00', '19:00')])
    for (const handle of [...screen.getAllByTestId('resize-start'), ...screen.getAllByTestId('resize-end')]) {
      expect(handle.getAttribute('tabindex')).toBe('-1')
      expect(handle.getAttribute('aria-hidden')).toBe('true')
    }
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

  it('lets a phone-week row be long-pressed and dropped on another day', () => {
    mockPointerIsFine = false
    const { container } = renderWeek([allDayFrom('Anniversaire', 'EVENT')])
    const rows = [...container.querySelectorAll('[data-testid="week-day-section"] [data-draggable]')]
    expect(rows).toHaveLength(1)
    expect(rows[0].getAttribute('data-drag-origin')).toBe('row')
  })

  it('draws a moved block at its landing slot — right day, right time — and dims the original', () => {
    const piano = timed('Piano', '18:00', '19:00')
    const { container } = renderWeek([piano], true, {
      intent: { kind: 'move', occurrence: piano, from: 'grid', day: '2026-01-06' },
      change: { allDay: false, startDate: '2026-01-07', startTime: '09:00', endDate: '2026-01-07', endTime: '10:00' },
    })
    const columns = columnsOf(container)
    const landing = within(columns[2]).getByTestId('drag-preview')
    expect(landing.style.top).toBe('540px')
    expect(landing.style.height).toBe('60px')
    expect(landing.textContent).toContain('09:00 – 10:00')
    expect(within(columns[1]).queryByTestId('drag-preview')).toBeNull()
    expect(positionOf(within(columns[1]).getByRole('button', { name: /Piano/ }))?.className).toContain('opacity-40')
  })

  it('stretches the block itself while its end is dragged', () => {
    const piano = timed('Piano', '18:00', '19:00')
    const { container } = renderWeek([piano], true, {
      intent: { kind: 'resize-end', occurrence: piano },
      change: { allDay: false, startDate: '2026-01-06', startTime: '18:00', endDate: '2026-01-06', endTime: '20:00' },
    })
    const tuesday = columnsOf(container)[1]
    const stretched = within(tuesday).getByTestId('drag-preview')
    expect(stretched.style.top).toBe('1080px')
    expect(stretched.style.height).toBe('120px')
    // The copy is the stretch: the original is hidden behind it, not shown twice.
    expect(positionOf(within(tuesday).getByRole('button', { name: /Piano/ }))?.className).toContain('opacity-0')
  })

  it('draws a block dropped on a band as an all-day item in that day\'s band', () => {
    const piano = timed('Piano', '18:00', '19:00')
    const { container } = renderWeek([piano], true, {
      intent: { kind: 'move', occurrence: piano, from: 'grid', day: '2026-01-06' },
      change: { allDay: true, startDate: '2026-01-08', startTime: null, endDate: '2026-01-08', endTime: null },
    })
    const bands = [...container.querySelectorAll<HTMLElement>('[data-testid="all-day-band"]')]
    expect(within(bands[3]).getByTestId('drag-preview').textContent).toContain('Piano')
    expect(bands.filter((band) => within(band).queryByTestId('drag-preview'))).toHaveLength(1)
    expect(columnsOf(container).some((column) => within(column).queryByTestId('drag-preview'))).toBe(false)
  })

  it('draws a moved phone row in the section of its landing day, and dims the original', () => {
    const birthday = allDayFrom('Anniversaire', 'EVENT')
    const { container } = renderWeek([birthday], true, {
      intent: { kind: 'move', occurrence: birthday, from: 'row', day: '2026-01-06' },
      change: { allDay: true, startDate: '2026-01-09', startTime: null, endDate: '2026-01-09', endTime: null },
    })
    const sections = [...container.querySelectorAll<HTMLElement>('[data-testid="week-day-section"]')]
    expect(within(sections[4]).getByTestId('drag-preview').textContent).toContain('Anniversaire')
    expect(within(sections[1]).getByRole('button', { name: /Anniversaire/ }).className).toContain('opacity-40')
  })

  it('draws no copy while the item hovers its own place', () => {
    const piano = timed('Piano', '18:00', '19:00')
    const { container } = renderWeek([piano], true, {
      intent: { kind: 'move', occurrence: piano, from: 'grid', day: '2026-01-06' },
      change: { allDay: false, startDate: '2026-01-06', startTime: '18:00', endDate: '2026-01-06', endTime: '19:00' },
    })
    expect(container.querySelectorAll('[data-testid="drag-preview"]')).toHaveLength(0)
  })

  it('draws each day\'s divider down the whole day, not only the first screenful', () => {
    // In a scrolling flex row, a stretched column is only as tall as the scroller: its border
    // stopped wherever the first 60vh ended.
    const { container } = renderWeek([])
    const scroller = columnsOf(container)[0].parentElement?.parentElement as HTMLElement
    expect(scroller.className).toContain('overflow-y-auto')
    expect(scroller.className).toContain('items-start')
  })

  it('keeps the day headers and bands lined up with the hour columns beside a scrollbar', () => {
    // The grid below scrolls and its scrollbar takes width; rows above that did not reserve the
    // same gutter drifted by up to its width at Sunday — a landing preview then sat off its column.
    const { container } = renderWeek([])
    const [header, band, grid] = [...container.querySelectorAll<HTMLElement>('.md\\:block > div')]
    expect(grid.className).toContain('overflow-y-auto')
    for (const row of [header, band]) {
      expect(row.className).toContain('[scrollbar-gutter:stable]')
      expect(row.className).toContain('overflow-hidden')
    }
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

  it('keeps the hour grid on a day with no timed event, so a drag carried there still lands on a time', () => {
    // Without it, an event dragged across the edge to an empty day could only land in the band —
    // and silently become all-day. Its unmounting also reset the scroll to midnight mid-drag.
    const { container } = renderDay([allDayFrom('Poubelles', 'TASK')])
    expect(container.querySelector('[style*="height: 1440px"]')).toBeTruthy()
  })

  it('says a day is empty over its grid, never above it — the grid must not jump under a drag', () => {
    // Paging to an empty day mid-drag used to insert the message above the grid, shifting every
    // time under the pointer by about an hour.
    renderDay([])
    const message = screen.getByText('empty_day')
    expect(message.closest('[data-testid="day-grid"]')).toBeTruthy()
    expect(message.className).toContain('absolute')
  })

  it('draws the day\'s divider down the whole day', () => {
    const { container } = renderDay([])
    const column = container.querySelector('[style*="height: 1440px"]') as HTMLElement
    const scroller = column.parentElement?.parentElement as HTMLElement
    expect(scroller.className).toContain('overflow-y-auto')
    expect(scroller.className).toContain('items-start')
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
