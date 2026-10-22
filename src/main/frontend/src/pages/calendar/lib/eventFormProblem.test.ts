import { describe, it, expect } from 'vitest'
import type { EventInput } from '@/entities/calendar'
import { eventFormProblem } from './eventFormProblem'
import type { Recurrence } from './seriesInput'

const event: EventInput = {
  title: 'Piano', description: null, location: null, allDay: false,
  startDate: '2026-10-02', startTime: '18:00', endDate: '2026-10-02', endTime: '19:00',
  color: null, participantIds: [],
}
const weekly: Recurrence = { intervalType: 'WEEKLY', intervalCount: 1, until: null, weekdays: ['FRIDAY'], dayRule: 'SAME_DAY' }
const key = (input: EventInput, recurrence: Recurrence | null = null) => eventFormProblem(input, recurrence)?.key ?? null

describe('eventFormProblem', () => {
  it('finds nothing wrong with a well-formed event or series', () => {
    expect(eventFormProblem(event, null)).toBeNull()
    expect(eventFormProblem(event, weekly)).toBeNull()
  })

  it('wants a title other than spaces', () => {
    expect(key({ ...event, title: '  ' })).toBe('form.title_required')
  })

  it('wants both dates, a cleared one included', () => {
    expect(key({ ...event, startDate: '' })).toBe('form.dates_required')
    expect(key({ ...event, endDate: '' })).toBe('form.dates_required')
  })

  it('refuses an end before the start, by date or on one day by time', () => {
    expect(key({ ...event, endDate: '2026-10-01' })).toBe('form.end_before_start')
    expect(key({ ...event, endTime: '17:00' })).toBe('form.end_before_start')
  })

  it('takes an earlier end time across days as an overnight event', () => {
    expect(key({ ...event, startTime: '22:00', endDate: '2026-10-03', endTime: '02:00' })).toBeNull()
  })

  it('wants both times of an event not on the whole day', () => {
    expect(key({ ...event, endTime: null })).toBe('form.times_required')
  })

  it('refuses an event covering more days than the server allows, and says how many', () => {
    expect(eventFormProblem({ ...event, allDay: true, startTime: null, endTime: null, endDate: '2027-10-03' }, null))
      .toEqual({ key: 'form.too_long', values: { days: 366 } })
  })

  it('wants an interval of at least 1', () => {
    expect(key(event, { ...weekly, intervalCount: 0 })).toBe('series.interval_invalid')
    expect(key(event, { ...weekly, intervalCount: Number('') })).toBe('series.interval_invalid')
  })

  it('refuses an occurrence running into the next one', () => {
    expect(key({ ...event, endDate: '2026-10-10' }, weekly)).toBe('series.occurrence_too_long')
  })

  it('refuses a series that stops before it starts', () => {
    expect(key(event, { ...weekly, until: '2026-10-01' })).toBe('series.until_before_start')
  })

  it('checks nothing of a series for a plain event', () => {
    expect(key({ ...event, endDate: '2026-10-10' })).toBeNull()
  })
})
