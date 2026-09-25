import { describe, it, expect } from 'vitest'
import type { EventInput } from '@/entities/calendar'
import { coversTooManyDays, occurrenceOutlastsInterval } from './scheduleLimits'
import type { Recurrence } from './seriesInput'

const day: EventInput = {
  title: 'x', description: null, location: null, allDay: true,
  startDate: '2026-01-01', startTime: null, endDate: '2026-01-01', endTime: null, color: null, participantIds: [],
}
const every = (intervalType: Recurrence['intervalType'], intervalCount = 1): Recurrence => ({ intervalType, intervalCount, until: null })

describe('coversTooManyDays', () => {
  it('accepts an event covering 366 days, and no more', () => {
    expect(coversTooManyDays({ ...day, endDate: '2027-01-01' })).toBe(false)
    expect(coversTooManyDays({ ...day, endDate: '2027-01-02' })).toBe(true)
  })
})

describe('occurrenceOutlastsInterval', () => {
  it('lets a weekly occurrence fill its week, and not run into the next', () => {
    expect(occurrenceOutlastsInterval({ ...day, endDate: '2026-01-07' }, every('WEEKLY'))).toBe(false)
    expect(occurrenceOutlastsInterval({ ...day, endDate: '2026-01-08' }, every('WEEKLY'))).toBe(true)
  })

  it('lets a nightly occurrence run past midnight', () => {
    const night = { ...day, allDay: false, startTime: '22:00', endDate: '2026-01-02', endTime: '02:00' }
    expect(occurrenceOutlastsInterval(night, every('DAILY'))).toBe(false)
  })

  it('refuses a timed occurrence overlapping the next by a quarter of an hour', () => {
    const long = { ...day, allDay: false, startTime: '09:00', endDate: '2026-01-02', endTime: '09:15' }
    expect(occurrenceOutlastsInterval(long, every('DAILY'))).toBe(true)
  })

  it('reckons a month at its shortest, as the server does', () => {
    expect(occurrenceOutlastsInterval({ ...day, endDate: '2026-01-28' }, every('MONTHLY'))).toBe(false)
    expect(occurrenceOutlastsInterval({ ...day, endDate: '2026-01-29' }, every('MONTHLY'))).toBe(true)
  })

  it('reads the API times, seconds included', () => {
    const shift = { ...day, allDay: false, startTime: '22:00:00', endDate: '2026-01-02', endTime: '02:00:00' }
    expect(occurrenceOutlastsInterval(shift, every('DAILY'))).toBe(false)
  })
})
