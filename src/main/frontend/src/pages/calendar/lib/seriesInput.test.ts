import { describe, it, expect } from 'vitest'
import type { EventInput, RecurringEventSeries } from '@/entities/calendar'
import { fromSeries, runsAcross, toSeriesInput, type Recurrence } from './seriesInput'

const event: EventInput = {
  title: 'Piano', description: 'Salle 3', location: 'Conservatoire', allDay: false,
  startDate: '2026-10-02', startTime: '18:00', endDate: '2026-10-02', endTime: '19:00',
  color: 'status-blue', participantIds: ['u-1', 'u-2'],
}
const weekly: Recurrence = { intervalType: 'WEEKLY', intervalCount: 1, until: null }

describe('toSeriesInput', () => {
  it('keeps everything an event says, and starts the series on its first day', () => {
    expect(toSeriesInput(event, { intervalType: 'MONTHLY', intervalCount: 2, until: '2027-06-30' })).toEqual({
      title: 'Piano', description: 'Salle 3', location: 'Conservatoire', allDay: false,
      startTime: '18:00', endTime: '19:00', durationDays: 0, color: 'status-blue',
      intervalType: 'MONTHLY', intervalCount: 2, anchorDate: '2026-10-02', endDate: '2027-06-30',
      participantIds: ['u-1', 'u-2'],
    })
  })

  it('makes an event lasting several days a series whose every occurrence lasts as long', () => {
    const weekend = { ...event, allDay: true, startTime: null, endTime: null, startDate: '2026-10-02', endDate: '2026-10-04' }
    expect(toSeriesInput(weekend, weekly)).toMatchObject({ allDay: true, startTime: null, endTime: null, durationDays: 2 })
  })

  it('carries an evening running past midnight as one day past its start', () => {
    const evening = { ...event, startTime: '22:00', endDate: '2026-10-03', endTime: '02:00' }
    expect(toSeriesInput(evening, weekly)).toMatchObject({ startTime: '22:00', endTime: '02:00', durationDays: 1 })
  })
})

describe('fromSeries', () => {
  const series: RecurringEventSeries = {
    id: 's-1', title: 'Piano', description: 'Salle 3', location: 'Conservatoire', allDay: false,
    startTime: '22:00:00', endTime: '02:00:00', durationDays: 1, color: 'status-blue',
    intervalType: 'WEEKLY', intervalCount: 2, anchorDate: '2026-10-02', endDate: '2027-06-30',
    startsOn: null, firstDate: '2026-10-02',
    participantIds: ['u-2'], createdBy: 'u-2', createdAt: '2026-01-01T00:00:00Z',
  }

  it('reads a series back as its first occurrence and how it repeats', () => {
    expect(fromSeries(series)).toEqual({
      event: {
        title: 'Piano', description: 'Salle 3', location: 'Conservatoire', allDay: false,
        startDate: '2026-10-02', startTime: '22:00:00', endDate: '2026-10-03', endTime: '02:00:00',
        color: 'status-blue', participantIds: ['u-2'],
      },
      recurrence: { intervalType: 'WEEKLY', intervalCount: 2, until: '2027-06-30' },
    })
  })

  it('gives back the same series once written again', () => {
    const { event: read, recurrence } = fromSeries(series)
    // Laid over the series, the written fields change nothing — and add nothing it does not have.
    expect({ ...series, ...toSeriesInput(read, recurrence) }).toEqual(series)
  })
})

/** Weekly from Monday Sept 7, carrying on an edit from Thursday Sept 24: first shown on Monday Sept 28. */
const carriedOn: RecurringEventSeries = {
  id: 's-2', title: 'Piano', description: null, location: null, allDay: true, startTime: null, endTime: null,
  durationDays: 0, color: null, intervalType: 'WEEKLY', intervalCount: 1, anchorDate: '2026-09-07', endDate: null,
  startsOn: '2026-09-24', firstDate: '2026-09-28', participantIds: [], createdBy: 'u-1', createdAt: '2026-01-01T00:00:00Z',
}

describe('a series carrying on an edit', () => {
  it('opens on its first occurrence shown, not on the anchor that sets its rhythm', () => {
    expect(fromSeries(carriedOn).event.startDate).toBe('2026-09-28')
  })

  it('keeps its rhythm when its start is left alone', () => {
    const { event, recurrence } = fromSeries(carriedOn)
    expect(toSeriesInput({ ...event, title: 'Piano (salle 3)' }, recurrence, carriedOn).anchorDate).toBe('2026-09-07')
  })

  it('takes the rhythm of a start that was moved', () => {
    const { event, recurrence } = fromSeries(carriedOn)
    expect(toSeriesInput({ ...event, startDate: '2026-09-29', endDate: '2026-09-29' }, recurrence, carriedOn).anchorDate)
      .toBe('2026-09-29')
  })
})

describe('runsAcross', () => {
  it('holds once a series has begun before today and is not over', () => {
    expect(runsAcross({ ...carriedOn, firstDate: '2026-09-01' }, '2026-09-24')).toBe(true)
    expect(runsAcross({ ...carriedOn, firstDate: '2026-09-01', endDate: '2026-09-24' }, '2026-09-24')).toBe(true)
  })

  it('does not hold for a series not begun yet, over already, or with nothing left', () => {
    expect(runsAcross({ ...carriedOn, firstDate: '2026-09-24' }, '2026-09-24')).toBe(false)
    expect(runsAcross({ ...carriedOn, firstDate: '2026-09-01', endDate: '2026-09-23' }, '2026-09-24')).toBe(false)
    expect(runsAcross({ ...carriedOn, firstDate: null }, '2026-09-24')).toBe(false)
  })
})
