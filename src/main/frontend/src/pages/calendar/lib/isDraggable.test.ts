import { describe, it, expect } from 'vitest'
import { isDraggable } from './isDraggable'
import type { CalendarOccurrence, CalendarSourceType } from '@/entities/calendar'

function occ(source: CalendarSourceType, materialized: boolean, series?: { seriesId: string; originalDate: string }): CalendarOccurrence {
  return {
    source, sourceId: 'x', seriesId: series?.seriesId ?? null, originalDate: series?.originalDate ?? null,
    materialized, title: 'x', description: null, location: null, allDay: true,
    startDate: '2026-01-01', startTime: null, endDate: '2026-01-01', endTime: null, color: null, participantIds: [],
  }
}

describe('isDraggable', () => {
  it('moves a one-off event, a task and a meal', () => {
    expect(isDraggable(occ('EVENT', true))).toBe(true)
    expect(isDraggable(occ('TASK', true))).toBe(true)
    expect(isDraggable(occ('MEAL', true))).toBe(true)
  })

  it('moves a projected occurrence of an event series — it detaches just that slot', () => {
    expect(isDraggable(occ('EVENT', false, { seriesId: 's-1', originalDate: '2026-01-01' }))).toBe(true)
  })

  it('does not move the future occurrence of a recurring task, which has no row and no slot mechanism', () => {
    expect(isDraggable(occ('TASK', false, { seriesId: 's-1', originalDate: '2026-01-01' }))).toBe(false)
  })

  it('never moves finance or savings', () => {
    expect(isDraggable(occ('FINANCE', true))).toBe(false)
    expect(isDraggable(occ('SAVINGS', true))).toBe(false)
  })
})
