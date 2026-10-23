import { describe, expect, it } from 'vitest'
import type { CalendarOccurrence } from '@/entities/calendar'
import { formatWhen } from './occurrenceWhen'

const base: CalendarOccurrence = {
  source: 'EXTERNAL', sourceId: 'f1:x:2026-10-17T00:00', seriesId: null, originalDate: null, materialized: true,
  title: 'x', description: null, location: null, allDay: true, startDate: '2026-10-17', startTime: null,
  endDate: '2026-10-17', endTime: null, color: 'event-cyan', participantIds: [], externalFeedId: 'f1',
}

describe('formatWhen', () => {
  it('says an all-day entry of one day as its day, all day', () => {
    expect(formatWhen(base, 'toute la journée', 'fr')).toMatch(/17 octobre 2026 · toute la journée$/)
  })

  it('says an entry over several days as its span', () => {
    expect(formatWhen({ ...base, endDate: '2026-11-01' }, 'toute la journée', 'fr'))
      .toBe('17 oct. – 1 nov. 2026 · toute la journée')
  })

  it('says a timed entry with its hours, without seconds', () => {
    expect(formatWhen({ ...base, allDay: false, startTime: '09:30:00', endTime: '11:00:00' }, 'toute la journée', 'fr'))
      .toMatch(/ · 09:30 – 11:00$/)
  })
})
