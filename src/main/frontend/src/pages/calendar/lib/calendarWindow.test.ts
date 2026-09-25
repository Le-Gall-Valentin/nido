import { describe, it, expect } from 'vitest'
import {
  addDays, addMonths, daysBetween, groupByDay, isValidIso, monthGridDates, weekDates, windowFor,
} from './calendarWindow'
import type { CalendarOccurrence } from '@/entities/calendar'

function occurrence(overrides: Partial<CalendarOccurrence>): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: 'x', seriesId: null, originalDate: null, materialized: true,
    title: 'x', description: null, location: null, allDay: true, startDate: '2026-01-01', startTime: null, endDate: '2026-01-01',
    endTime: null, color: null, participantIds: [], ...overrides,
  }
}

describe('monthGridDates', () => {
  it('covers a month with six Monday-started weeks, always 42 days', () => {
    const dates = monthGridDates('2026-02-14')
    expect(dates).toHaveLength(42)
    expect(dates[0]).toBe('2026-01-26')
    expect(dates).toContain('2026-02-28')
  })

  it('starts on the Monday on or before the first of the month', () => {
    // 2026-03-01 is a Sunday, so the grid opens on the Monday of the week containing it.
    expect(monthGridDates('2026-03-15')[0]).toBe('2026-02-23')
  })

  it('keeps a constant height so the grid never jumps between months', () => {
    expect(monthGridDates('2026-02-01')).toHaveLength(42)
    expect(monthGridDates('2026-08-01')).toHaveLength(42)
  })
})

describe('weekDates', () => {
  it('returns seven days starting on Monday', () => {
    expect(weekDates('2026-01-08')).toEqual([
      '2026-01-05', '2026-01-06', '2026-01-07', '2026-01-08',
      '2026-01-09', '2026-01-10', '2026-01-11'])
  })

  it('treats Sunday as the last day of its week, not the first', () => {
    expect(weekDates('2026-01-11')[0]).toBe('2026-01-05')
  })
})

describe('windowFor', () => {
  it('asks the feed for the whole month grid, not just the month', () => {
    expect(windowFor('month', '2026-02-14')).toEqual({ from: '2026-01-26', to: '2026-03-08' })
  })

  it('asks for exactly the week in week view and the single day in day view', () => {
    expect(windowFor('week', '2026-01-08')).toEqual({ from: '2026-01-05', to: '2026-01-11' })
    expect(windowFor('day', '2026-01-08')).toEqual({ from: '2026-01-08', to: '2026-01-08' })
  })

  it('never asks for more days than the backend allows', () => {
    const { from, to } = windowFor('month', '2026-02-14')
    expect(daysBetween(from, to) + 1).toBeLessThanOrEqual(366)
  })
})

describe('addMonths', () => {
  it('clamps to the target month rather than spilling over', () => {
    // January 31 + one month is February 28, not March 3.
    expect(addMonths('2026-01-31', 1)).toBe('2026-02-28')
  })

  it('steps back a month the same way', () => {
    expect(addMonths('2026-03-31', -1)).toBe('2026-02-28')
  })

  it('crosses a year boundary', () => {
    expect(addMonths('2026-12-15', 1)).toBe('2027-01-15')
  })
})

describe('isValidIso', () => {
  it.each(['2026-01-01', '2026-02-28', '2028-02-29'])('accepts %s', (value) => {
    expect(isValidIso(value)).toBe(true)
  })

  it.each(['2026-02-30', '2026-13-01', '2026-1-1', 'yesterday', ''])('rejects %s', (value) => {
    expect(isValidIso(value)).toBe(false)
  })
})

describe('addDays and daysBetween', () => {
  it('crosses a daylight-saving boundary without drifting', () => {
    // Late March is where a local-time implementation loses or gains an hour and slips a day.
    expect(addDays('2026-03-28', 1)).toBe('2026-03-29')
    expect(addDays('2026-03-29', 1)).toBe('2026-03-30')
    expect(daysBetween('2026-03-28', '2026-03-30')).toBe(2)
  })
})

describe('groupByDay', () => {
  it('puts a multi-day occurrence on every day it spans', () => {
    const holidays = occurrence({ startDate: '2026-07-01', endDate: '2026-07-03', title: 'Vacances' })
    const grouped = groupByDay([holidays], ['2026-07-01', '2026-07-02', '2026-07-03', '2026-07-04'])

    expect(grouped.get('2026-07-01')).toEqual([holidays])
    expect(grouped.get('2026-07-02')).toEqual([holidays])
    expect(grouped.get('2026-07-03')).toEqual([holidays])
    expect(grouped.get('2026-07-04')).toBeUndefined()
  })

  it('keeps the part of a span that falls inside the window and drops the rest', () => {
    const holidays = occurrence({ startDate: '2026-06-28', endDate: '2026-07-05' })
    const grouped = groupByDay([holidays], ['2026-07-01', '2026-07-02'])

    expect([...grouped.keys()]).toEqual(['2026-07-01', '2026-07-02'])
  })

  it('groups several occurrences on the same day', () => {
    const a = occurrence({ title: 'A', startDate: '2026-01-05', endDate: '2026-01-05' })
    const b = occurrence({ title: 'B', startDate: '2026-01-05', endDate: '2026-01-05' })
    expect(groupByDay([a, b], ['2026-01-05']).get('2026-01-05')).toEqual([a, b])
  })

  it('returns nothing for a day with no occurrence', () => {
    expect(groupByDay([], ['2026-01-05']).get('2026-01-05')).toBeUndefined()
  })

  it('costs the days shown, not the days an occurrence lasts', () => {
    // An event may no longer cover more than 366 days, but one written before that rule, or by
    // anything else than the form, must still not freeze the month: this took 3 s per render.
    const forever = occurrence({ startDate: '0001-01-01', endDate: '9999-12-31' })
    const month = monthGridDates('2026-09-15')
    const started = performance.now()
    const grouped = groupByDay([forever], month)
    expect(performance.now() - started).toBeLessThan(100)
    expect([...grouped.keys()]).toEqual(month)
  })

  it('does not show an event ending at exactly midnight on the day after', () => {
    // A 22:00 → 00:00 evening used to appear on two days in the month view.
    const evening = occurrence({ allDay: false, startDate: '2026-09-23', startTime: '22:00',
      endDate: '2026-09-24', endTime: '00:00' })
    const grouped = groupByDay([evening], ['2026-09-23', '2026-09-24'])
    expect(grouped.get('2026-09-23')).toEqual([evening])
    expect(grouped.get('2026-09-24')).toBeUndefined()
  })
})
