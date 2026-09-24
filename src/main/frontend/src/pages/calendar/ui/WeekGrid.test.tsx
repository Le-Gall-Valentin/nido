import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, within, fireEvent } from '@testing-library/react'
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
  return [...container.querySelectorAll<HTMLElement>('[data-testid="hour-column"]')]
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

  it('draws a timed event lasting several days in the grid, one piece per day, never in the bands', () => {
    const conference = occurrence({ title: 'Congrès', allDay: false, startDate: '2026-01-05', startTime: '09:00',
      endDate: '2026-01-07', endTime: '17:00' })
    const { container } = renderWeek([conference])
    const columns = columnsOf(container)
    const pieceIn = (column: HTMLElement) => positionOf(within(column).getByRole('button', { name: /Congrès/ }))
    expect([0, 1, 2].map((i) => `${pieceIn(columns[i])?.style.top}/${pieceIn(columns[i])?.style.height}`))
      .toEqual(['540px/900px', '0px/1440px', '0px/1020px'])
    expect(container.querySelectorAll('[data-testid="all-day-band"] button')).toHaveLength(0)
    // Stretched from its true start and its true end only.
    expect(within(columns[0]).queryAllByTestId('resize-start')).toHaveLength(1)
    expect(within(columns[2]).queryAllByTestId('resize-end')).toHaveLength(1)
    expect(screen.getAllByTestId('resize-start')).toHaveLength(1)
    expect(screen.getAllByTestId('resize-end')).toHaveLength(1)
  })

  it('keeps a title at the top of its piece, and in view while a long piece scrolls past', () => {
    // Centred in a fifteen-hour piece, the title sat hours away from what was on screen.
    const { container } = renderWeek([occurrence({ title: 'Congrès', allDay: false, startDate: '2026-01-05',
      startTime: '09:00', endDate: '2026-01-07', endTime: '17:00' })])
    const block = within(columnsOf(container)[0]).getByRole('button', { name: /Congrès/ })
    expect(block.className).toContain('justify-start')
    // overflow: clip, not hidden — hidden would make the block its own scroller and pin the title to it.
    expect(block.className).toContain('overflow-clip')
    expect(block.className).not.toContain('overflow-hidden')
    expect(within(block).getByText('Congrès').closest('.sticky')).toBeTruthy()
  })

  it('offsets two overlapping events so they stay told apart, even in the same colour', () => {
    const { container } = renderWeek([timed('Piano', '18:00', '19:30'), timed('Chorale', '18:30', '20:00')])
    const tuesday = columnsOf(container)[1]
    const box = (name: RegExp) => positionOf(within(tuesday).getByRole('button', { name })) as HTMLElement
    expect([box(/Piano/).style.left, box(/Piano/).style.width]).toEqual(['0%', '80%'])
    expect([box(/Chorale/).style.left, box(/Chorale/).style.width]).toEqual(['50%', '50%'])
    // The later one is drawn over the earlier, edged in the page's colour so the two never merge.
    expect(Number(box(/Chorale/).style.zIndex)).toBeGreaterThan(Number(box(/Piano/).style.zIndex))
    expect(within(box(/Chorale/)).getByRole('button', { name: /Chorale/ }).className).toContain('ring-bg-1')
  })

  it('keeps an event that overlaps nothing at the full width of its column', () => {
    const { container } = renderWeek([timed('Piano', '18:00', '19:00'), timed('Chorale', '19:00', '20:00')])
    const tuesday = columnsOf(container)[1]
    for (const name of [/Piano/, /Chorale/]) {
      const box = positionOf(within(tuesday).getByRole('button', { name })) as HTMLElement
      expect([box.style.left, box.style.width]).toEqual(['0%', '100%'])
      expect(within(box).getByRole('button', { name }).className).not.toContain('ring-bg-1')
    }
  })

  it('counts a very short event by the room it takes on screen', () => {
    // Drawn 20 minutes tall, a zero-length reminder at 18:00 would hide under an 18:10 event.
    const { container } = renderWeek([timed('Rappel', '18:00', '18:00'), timed('Piano', '18:10', '19:00')])
    const tuesday = columnsOf(container)[1]
    const left = (name: RegExp) => (positionOf(within(tuesday).getByRole('button', { name })) as HTMLElement).style.left
    expect([left(/Rappel/), left(/Piano/)]).toEqual(['0%', '50%'])
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
    // Title and times stick in view over a long landing piece too.
    expect(within(landing).getByText('Piano').parentElement?.className).toContain('sticky')
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

describe('what a block says: everything that fits, in whole lines', () => {
  const concert = (endTime: string, details: Partial<CalendarOccurrence> = { location: 'Salle Pleyel', description: 'Apporter les billets' }) =>
    occurrence({ title: 'Concert', allDay: false, startTime: '18:00', endTime, location: null, description: null, ...details })
  const blockOf = (container: HTMLElement) => within(columnsOf(container)[1]).getByRole('button', { name: /Concert/ })

  it('fits the title and the start on one line when only one line fits', () => {
    // 30 minutes: 30px, less 4px of padding, holds one 15px line.
    const { container } = renderWeek([concert('18:30')])
    expect(blockOf(container).textContent).toBe('Concert · 18:00')
  })

  it('writes the times under the title once two lines fit', () => {
    const { container } = renderWeek([concert('18:45')])
    const block = blockOf(container)
    expect(within(block).getByText('18:00 – 18:45')).toBeTruthy()
    expect(within(block).queryByText('Salle Pleyel')).toBeNull()
  })

  it('adds the place as soon as a third line fits — an hour is enough', () => {
    const { container } = renderWeek([concert('19:00')])
    const block = blockOf(container)
    expect(within(block).getByText('Salle Pleyel')).toBeTruthy()
    expect(within(block).queryByText('Apporter les billets')).toBeNull()
  })

  it('gives the third line to the description when there is no place', () => {
    const { container } = renderWeek([concert('19:00', { description: 'Apporter les billets' })])
    expect(within(blockOf(container)).getByText('Apporter les billets').style.maxHeight).toBe('15px')
  })

  it('gives the description every whole line left', () => {
    const { container } = renderWeek([concert('20:30')])
    // 150px: 4px of padding leaves nine 15px lines; title, times and place take three.
    expect(within(blockOf(container)).getByText('Apporter les billets').style.maxHeight).toBe('90px')
  })

  it('says only what an event has — no empty place line', () => {
    const { container } = renderWeek([concert('21:00', {})])
    expect(blockOf(container).textContent).toBe('Concert18:00 – 21:00')
  })
})

describe('where the hour grid opens', () => {
  // jsdom lays nothing out: the grid's visible height is given, nine hours as on a desktop.
  beforeEach(() => { vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(540) })
  afterEach(() => { vi.restoreAllMocks() })

  const on = (title: string, startDate: string, startTime: string, endTime: string) =>
    occurrence({ title, allDay: false, startDate, startTime, endDate: startDate, endTime })
  const week = (date: string, occurrences: CalendarOccurrence[], drag: DragPreviewState | null = null) => (
    <DndContext>
      <DragPreviewProvider value={drag}>
        <WeekGrid date={date} occurrences={occurrences} today="2026-01-06" canWrite
          onSelectDay={vi.fn()} onSelectOccurrence={vi.fn()} />
      </DragPreviewProvider>
    </DndContext>)
  const scrollerOf = (container: HTMLElement) =>
    columnsOf(container)[0].parentElement?.parentElement as HTMLElement

  const busyMorning = [on('A', '2026-01-06', '09:00', '10:00'), on('B', '2026-01-07', '10:00', '11:00'),
    on('C', '2026-01-08', '11:00', '12:00'), on('D', '2026-01-09', '19:00', '20:00')]

  it('opens on the busiest stretch of the week, not at midnight', () => {
    const { container } = render(week('2026-01-08', busyMorning))
    expect(scrollerOf(container).scrollTop).toBe(510)   // 08:30, half an hour before the first
  })

  it('opens at 08:00 on a week with nothing timed', () => {
    const { container } = render(week('2026-01-08', []))
    expect(scrollerOf(container).scrollTop).toBe(480)
  })

  it('never moves a grid the reader has scrolled, even when the week\'s events arrive after', () => {
    const { container, rerender } = render(week('2026-01-08', []))
    const scroller = scrollerOf(container)
    fireEvent.wheel(scroller)
    scroller.scrollTop = 900
    rerender(week('2026-01-08', busyMorning))
    expect(scroller.scrollTop).toBe(900)
  })

  it('opens the next week on its own busiest stretch', () => {
    const { container, rerender } = render(week('2026-01-08', busyMorning))
    const scroller = scrollerOf(container)
    fireEvent.wheel(scroller)
    rerender(week('2026-01-15', [on('E', '2026-01-13', '18:00', '19:00')]))
    expect(scroller.scrollTop).toBe(17.5 * 60)
  })

  it('does not move the grid when a drag carries an item into the next week', () => {
    const piano = on('Piano', '2026-01-06', '09:00', '10:00')
    const drag = { intent: { kind: 'move' as const, occurrence: piano, from: 'grid' as const, day: '2026-01-06' }, change: null }
    const { container, rerender } = render(week('2026-01-08', [piano], drag))
    const scroller = scrollerOf(container)
    scroller.scrollTop = 700
    rerender(week('2026-01-15', [on('E', '2026-01-13', '18:00', '19:00')], drag))
    rerender(week('2026-01-15', [on('E', '2026-01-13', '18:00', '19:00')]))
    expect(scroller.scrollTop).toBe(700)
  })

  it('opens the day view the same way', () => {
    const { container } = render(<DayAgenda date="2026-01-06" occurrences={[on('A', '2026-01-06', '14:00', '15:00')]}
      onSelectOccurrence={vi.fn()} />)
    const column = container.querySelector('[data-testid="hour-column"]') as HTMLElement
    expect((column.parentElement?.parentElement as HTMLElement).scrollTop).toBe(13.5 * 60)
  })
})

describe('picking out a time to add an event', () => {
  const mouse = { pointerType: 'mouse', button: 0 }

  function renderPicking(options: { canWrite?: boolean; occurrences?: CalendarOccurrence[] } = {}) {
    const onCreateRange = vi.fn()
    const canWrite = options.canWrite ?? true
    const view = render(
      <DndContext>
        <WeekGrid date="2026-01-08" occurrences={options.occurrences ?? []} today="2026-01-06" canWrite={canWrite}
          onSelectDay={vi.fn()} onSelectOccurrence={vi.fn()} onCreateRange={canWrite ? onCreateRange : undefined} />
      </DndContext>)
    const bands = [...view.container.querySelectorAll<HTMLElement>('[data-testid="all-day-band"]')]
    return { ...view, onCreateRange, columns: columnsOf(view.container), bands }
  }

  /** jsdom has no elementsFromPoint: say what the pointer is over once it leaves the first column. */
  function pointerOver(element: HTMLElement) {
    Object.defineProperty(document, 'elementsFromPoint', { value: () => [element], configurable: true })
  }
  afterEach(() => { Reflect.deleteProperty(document, 'elementsFromPoint') })

  // A column's top is 0 in jsdom, so a clientY is a number of minutes past midnight.
  it('opens a new event for an hour from the quarter hour clicked', () => {
    const { columns, onCreateRange } = renderPicking()
    fireEvent.pointerDown(columns[1], { ...mouse, clientY: 14 * 60 + 20 })
    fireEvent.pointerUp(window, { ...mouse, clientY: 14 * 60 + 20 })
    expect(onCreateRange).toHaveBeenCalledWith({
      allDay: false, startDate: '2026-01-06', startTime: '14:15', endDate: '2026-01-06', endTime: '15:15',
    })
  })

  it('draws the time being picked out, by quarter hours, and hands it over on release', () => {
    const { columns, onCreateRange } = renderPicking()
    fireEvent.pointerDown(columns[1], { ...mouse, clientY: 600 })
    fireEvent.pointerMove(window, { ...mouse, clientY: 700 })
    const picked = within(columns[1]).getByTestId('grid-selection')
    expect(picked.style.top).toBe('600px')
    expect(picked.style.height).toBe('105px')
    expect(picked.textContent).toContain('10:00 – 11:45')
    fireEvent.pointerUp(window, { ...mouse, clientY: 700 })
    expect(onCreateRange).toHaveBeenCalledWith({
      allDay: false, startDate: '2026-01-06', startTime: '10:00', endDate: '2026-01-06', endTime: '11:45',
    })
    expect(screen.queryByTestId('grid-selection')).toBeNull()
  })

  it('spans the days a drag crosses', () => {
    const { columns, onCreateRange } = renderPicking()
    fireEvent.pointerDown(columns[1], { ...mouse, clientY: 600 })
    pointerOver(columns[3])
    fireEvent.pointerMove(window, { ...mouse, clientY: 700 })
    expect(within(columns[2]).getByTestId('grid-selection').style.height).toBe('1440px')
    expect(within(columns[1]).getByTestId('grid-selection').textContent).toContain('→')
    fireEvent.pointerUp(window, { ...mouse, clientY: 700 })
    expect(onCreateRange).toHaveBeenCalledWith({
      allDay: false, startDate: '2026-01-06', startTime: '10:00', endDate: '2026-01-08', endTime: '11:45',
    })
  })

  it('starts nothing from an event — pressing an event is how it is dragged', () => {
    const { columns, onCreateRange } = renderPicking({ occurrences: [timed('Piano', '18:00', '19:00')] })
    fireEvent.pointerDown(within(columns[1]).getByRole('button', { name: /Piano/ }), { ...mouse, clientY: 1090 })
    fireEvent.pointerUp(window, { ...mouse, clientY: 1090 })
    expect(onCreateRange).not.toHaveBeenCalled()
  })

  it('lets a viewer pick nothing', () => {
    const { columns, onCreateRange } = renderPicking({ canWrite: false })
    fireEvent.pointerDown(columns[1], { ...mouse, clientY: 600 })
    fireEvent.pointerMove(window, { ...mouse, clientY: 700 })
    // Not even drawn while pressed: nothing is promised that could not be added.
    expect(screen.queryByTestId('grid-selection')).toBeNull()
    fireEvent.pointerUp(window, { ...mouse, clientY: 700 })
    expect(onCreateRange).not.toHaveBeenCalled()
  })

  it('leaves a finger alone — on a touch screen the grid scrolls', () => {
    const { columns, onCreateRange } = renderPicking()
    fireEvent.pointerDown(columns[1], { pointerType: 'touch', button: 0, clientY: 600 })
    fireEvent.pointerUp(window, { pointerType: 'touch', button: 0, clientY: 600 })
    expect(onCreateRange).not.toHaveBeenCalled()
  })

  it('drops the selection on Escape', () => {
    const { columns, onCreateRange } = renderPicking()
    fireEvent.pointerDown(columns[1], { ...mouse, clientY: 600 })
    fireEvent.pointerMove(window, { ...mouse, clientY: 700 })
    fireEvent.keyDown(window, { key: 'Escape' })
    fireEvent.pointerUp(window, { ...mouse, clientY: 700 })
    expect(onCreateRange).not.toHaveBeenCalled()
    expect(screen.queryByTestId('grid-selection')).toBeNull()
  })

  it('picks whole days in the all-day band', () => {
    const { bands, onCreateRange } = renderPicking()
    fireEvent.pointerDown(bands[0], mouse)
    pointerOver(bands[2])
    fireEvent.pointerMove(window, mouse)
    expect(bands.map((band) => band.hasAttribute('data-selected'))).toEqual([true, true, true, false, false, false, false])
    fireEvent.pointerUp(window, mouse)
    expect(onCreateRange).toHaveBeenCalledWith({
      allDay: true, startDate: '2026-01-05', startTime: null, endDate: '2026-01-07', endTime: null,
    })
  })

  it('works the same in the day view', () => {
    const onCreateRange = vi.fn()
    const { container } = render(
      <DayAgenda date="2026-01-06" occurrences={[]} canWrite onSelectOccurrence={vi.fn()} onCreateRange={onCreateRange} />)
    const column = container.querySelector('[data-testid="hour-column"]') as HTMLElement
    fireEvent.pointerDown(column, { ...mouse, clientY: 9 * 60 })
    fireEvent.pointerUp(window, { ...mouse, clientY: 9 * 60 })
    expect(onCreateRange).toHaveBeenCalledWith({
      allDay: false, startDate: '2026-01-06', startTime: '09:00', endDate: '2026-01-06', endTime: '10:00',
    })
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
    const column = container.querySelector('[data-testid="hour-column"]') as HTMLElement
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
