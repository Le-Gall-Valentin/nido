import { describe, it, expect } from 'vitest'
import { isDraggable } from './isDraggable'
import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'

function occ(source: CalendarSourceType, materialized: boolean): CalendarOccurrence {
  return {
    source, sourceId: 'x', seriesId: null, originalDate: null, materialized,
    title: 'x', description: null, location: null, allDay: true, startDate: '2026-01-01', startTime: null,
    endDate: '2026-01-01', endTime: null, color: null, participantIds: [],
  }
}

describe('isDraggable', () => {
  it.each<CalendarSourceType>(['EVENT', 'TASK', 'MEAL'])('lets you drag a materialized %s', (source) => {
    expect(isDraggable(occ(source, true))).toBe(true)
  })

  it.each<CalendarSourceType>(['EVENT', 'TASK', 'FINANCE', 'MEAL', 'SAVINGS'])(
    'refuses to drag a projected %s, which has no row to rewrite', (source) => {
      expect(isDraggable(occ(source, false))).toBe(false)
    })

  it('refuses to drag a finance transaction even though it has a row', () => {
    // Its date is an accounting fact, not a plan the calendar may rewrite by drag.
    expect(isDraggable(occ('FINANCE', true))).toBe(false)
  })

  it('refuses to drag a savings deadline, which belongs to the goal', () => {
    expect(isDraggable(occ('SAVINGS', true))).toBe(false)
  })
})
