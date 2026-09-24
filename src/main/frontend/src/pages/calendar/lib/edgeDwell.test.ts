import { describe, it, expect } from 'vitest'
import { IDLE, dragZone, horizontalZone, stepDwell, verticalZone, type DwellState } from './edgeDwell'
import type { CalendarOccurrence } from '@/entities/calendar'

function run(steps: Array<[zone: -1 | 0 | 1, now: number]>) {
  let state: DwellState = IDLE
  const fires: number[] = []
  for (const [zone, now] of steps) {
    const result = stepDwell(state, zone, now)
    state = result.state
    if (result.fire) fires.push(result.fire)
  }
  return fires
}

describe('stepDwell', () => {
  it('fires once the pointer has held the edge for 600ms', () => {
    expect(run([[1, 0], [1, 599]])).toEqual([])
    expect(run([[1, 0], [1, 600]])).toEqual([1])
  })

  it('repeats every 900ms while held, so several periods can be skipped', () => {
    expect(run([[1, 0], [1, 600], [1, 1499], [1, 1500], [1, 2400]])).toEqual([1, 1, 1])
  })

  it('starts over when the pointer leaves the edge', () => {
    expect(run([[1, 0], [0, 400], [1, 500], [1, 1000]])).toEqual([])
  })

  it('starts over when the pointer jumps to the other edge', () => {
    expect(run([[1, 0], [-1, 500], [-1, 1099], [-1, 1100]])).toEqual([-1])
  })
})

describe('horizontalZone', () => {
  const rect = { left: 100, right: 900 }
  it('arms only once the pointer leaves the calendar by a side', () => {
    expect(horizontalZone(900, rect)).toBe(1)
    expect(horizontalZone(1200, rect)).toBe(1)
    expect(horizontalZone(100, rect)).toBe(-1)
    expect(horizontalZone(40, rect)).toBe(-1)
  })

  it('never arms over the calendar itself — aiming at a Sunday must not page the week', () => {
    expect(horizontalZone(880, rect)).toBe(0)
    expect(horizontalZone(120, rect)).toBe(0)
    expect(horizontalZone(500, rect)).toBe(0)
  })
})

describe('dragZone', () => {
  const occurrence = { source: 'EVENT', sourceId: 'e1', seriesId: null, originalDate: null, materialized: true,
    title: 'x', description: null, location: null, allDay: false, startDate: '2026-09-23', startTime: '14:00',
    endDate: '2026-09-23', endTime: '15:00', color: null, participantIds: [] } as CalendarOccurrence
  const wide = { wide: true, container: { left: 100, right: 900 }, scroller: null }
  const pastRight = { x: 950, y: 400 }

  it('pages a move that leaves the calendar by a side, on a desktop', () => {
    expect(dragZone({ kind: 'move', occurrence, from: 'grid', day: '2026-09-23' }, pastRight, wide)).toBe(1)
  })

  it('never pages a resize — it stretches an event within its own day', () => {
    expect(dragZone({ kind: 'resize-end', occurrence }, pastRight, wide)).toBe(0)
    expect(dragZone({ kind: 'resize-start', occurrence }, { x: 50, y: 400 }, wide)).toBe(0)
  })

  it('never pages sideways on a phone, where the day view only changes time', () => {
    expect(dragZone({ kind: 'move', occurrence, from: 'grid', day: '2026-09-23' }, pastRight, { ...wide, wide: false })).toBe(0)
  })

  it('pages a phone-week row at the bottom once the list cannot scroll further', () => {
    const scroller = { top: 0, bottom: 800, canScrollUp: true, canScrollDown: false }
    expect(dragZone({ kind: 'move', occurrence, from: 'row', day: '2026-09-23' }, { x: 200, y: 790 },
      { wide: false, container: null, scroller })).toBe(1)
  })
})

describe('verticalZone', () => {
  const viewport = { top: 0, bottom: 800 }
  it('arms at the bottom only once there is nothing left to scroll down', () => {
    expect(verticalZone(790, viewport, true, true)).toBe(0)
    expect(verticalZone(790, viewport, true, false)).toBe(1)
  })

  it('arms at the top only once there is nothing left to scroll up', () => {
    expect(verticalZone(10, viewport, true, true)).toBe(0)
    expect(verticalZone(10, viewport, false, true)).toBe(-1)
  })
})
