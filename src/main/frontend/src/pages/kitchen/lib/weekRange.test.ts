import { describe, it, expect } from 'vitest'
import { startOfWeek, addDays, toISODate, weekDates } from './weekRange'

describe('startOfWeek', () => {
  it('returns the same Monday for every day in that week', () => {
    const monday = toISODate(startOfWeek(new Date('2026-09-09T10:00:00Z'))) // a Wednesday
    expect(monday).toBe('2026-09-07')
    expect(toISODate(startOfWeek(new Date('2026-09-13T10:00:00Z')))).toBe('2026-09-07') // the following Sunday
  })
})

describe('weekDates', () => {
  it('returns seven consecutive days starting from the given Monday', () => {
    const days = weekDates(new Date('2026-09-07T12:00:00Z')).map(toISODate)
    expect(days).toEqual([
      '2026-09-07', '2026-09-08', '2026-09-09', '2026-09-10', '2026-09-11', '2026-09-12', '2026-09-13',
    ])
  })
})

describe('addDays', () => {
  it('shifts forward and backward across week boundaries', () => {
    expect(toISODate(addDays(new Date('2026-09-07T12:00:00Z'), 7))).toBe('2026-09-14')
    expect(toISODate(addDays(new Date('2026-09-07T12:00:00Z'), -7))).toBe('2026-08-31')
  })
})
