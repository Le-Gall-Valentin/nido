import { describe, it, expect } from 'vitest'
import { covers, effectiveEndDate, isBandOccurrence, segmentFor } from './segments'
import type { CalendarOccurrence } from '@/entities/calendar'

function timed(startDate: string, startTime: string, endDate: string, endTime: string): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: 'x', seriesId: null, originalDate: null, materialized: true,
    title: 'x', description: null, location: null, allDay: false,
    startDate, startTime, endDate, endTime, color: null, participantIds: [],
  }
}

describe('segmentFor', () => {
  it('keeps a same-day event whole', () => {
    expect(segmentFor(timed('2026-09-23', '18:00', '2026-09-23', '19:30'), '2026-09-23'))
      .toEqual({ startMinutes: 1080, endMinutes: 1170, isStart: true, isEnd: true })
  })

  it('splits an evening that crosses midnight into two pieces', () => {
    const party = timed('2026-09-23', '22:00', '2026-09-24', '02:00')
    expect(segmentFor(party, '2026-09-23')).toEqual({ startMinutes: 1320, endMinutes: 1440, isStart: true, isEnd: false })
    expect(segmentFor(party, '2026-09-24')).toEqual({ startMinutes: 0, endMinutes: 120, isStart: false, isEnd: true })
  })

  it('does not put an event ending at exactly midnight on the next day', () => {
    const late = timed('2026-09-23', '22:00', '2026-09-24', '00:00')
    expect(segmentFor(late, '2026-09-23')).toEqual({ startMinutes: 1320, endMinutes: 1440, isStart: true, isEnd: true })
    expect(segmentFor(late, '2026-09-24')).toBeNull()
  })

  it('draws nothing for a day the event does not cover', () => {
    expect(segmentFor(timed('2026-09-23', '18:00', '2026-09-23', '19:00'), '2026-09-22')).toBeNull()
  })

  it('treats a daylight-saving date like any other: times are wall-clock strings', () => {
    // 2026-10-25 is 25 hours long in Europe/Paris. The grid shows wall-clock times, never instants.
    expect(segmentFor(timed('2026-10-25', '01:00', '2026-10-25', '04:00'), '2026-10-25'))
      .toEqual({ startMinutes: 60, endMinutes: 240, isStart: true, isEnd: true })
  })
})

describe('isBandOccurrence', () => {
  it('sends all-day items and timed events longer than a day to the band', () => {
    expect(isBandOccurrence({ ...timed('2026-09-23', '09:00', '2026-09-23', '10:00'), allDay: true })).toBe(true)
    expect(isBandOccurrence(timed('2026-09-23', '09:00', '2026-09-25', '10:00'))).toBe(true)
  })

  it('keeps an overnight event under 24 hours in the grid', () => {
    expect(isBandOccurrence(timed('2026-09-23', '22:00', '2026-09-24', '02:00'))).toBe(false)
  })
})

describe('effectiveEndDate', () => {
  it('pulls back an end at exactly 00:00 to the day before', () => {
    expect(effectiveEndDate(timed('2026-09-23', '22:00', '2026-09-24', '00:00'))).toBe('2026-09-23')
    expect(effectiveEndDate(timed('2026-09-23', '22:00', '2026-09-24', '02:00'))).toBe('2026-09-24')
  })
})

describe('covers', () => {
  it('says which days an occurrence occupies, the day after an end at 00:00 excluded', () => {
    const late = timed('2026-09-23', '22:00', '2026-09-24', '00:00')
    expect(covers(late, '2026-09-23')).toBe(true)
    expect(covers(late, '2026-09-24')).toBe(false)
    const party = timed('2026-09-23', '22:00', '2026-09-24', '02:00')
    expect(covers(party, '2026-09-24')).toBe(true)
    expect(covers(party, '2026-09-22')).toBe(false)
  })
})
