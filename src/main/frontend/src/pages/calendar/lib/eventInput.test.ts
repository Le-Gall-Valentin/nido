import { describe, it, expect } from 'vitest'
import { toEventInput, rescheduledInput } from './eventInput'
import type { CalendarOccurrence } from '@/entities/calendar'

const concert: CalendarOccurrence = {
  source: 'EVENT', sourceId: 'e1', seriesId: null, originalDate: null, materialized: true,
  title: 'Concert', description: 'Apporter les billets', location: 'Salle Pleyel',
  allDay: false, startDate: '2026-03-02', startTime: '20:00', endDate: '2026-03-02', endTime: '22:30',
  color: 'status-blue', participantIds: ['u1', 'u2'],
}

describe('toEventInput', () => {
  it('keeps every writable field, description and location included', () => {
    // Both the edit form and drag-to-reschedule start from this. A field dropped here is a field
    // erased on the next save — which is exactly what happened to description and location.
    expect(toEventInput(concert)).toEqual({
      title: 'Concert', description: 'Apporter les billets', location: 'Salle Pleyel',
      allDay: false, startDate: '2026-03-02', startTime: '20:00', endDate: '2026-03-02', endTime: '22:30',
      color: 'status-blue', participantIds: ['u1', 'u2'],
    })
  })
})

describe('rescheduledInput', () => {
  it('moves the event to the target day and keeps everything else', () => {
    const moved = rescheduledInput(concert, '2026-03-09')
    expect(moved.startDate).toBe('2026-03-09')
    expect(moved.endDate).toBe('2026-03-09')
    expect(moved.description).toBe('Apporter les billets')
    expect(moved.location).toBe('Salle Pleyel')
    expect(moved.startTime).toBe('20:00')
  })

  it('keeps a multi-day event the same length rather than resizing it', () => {
    const holidays = { ...concert, allDay: true, startTime: null, endTime: null,
      startDate: '2026-07-01', endDate: '2026-07-10' }
    const moved = rescheduledInput(holidays, '2026-07-15')
    expect(moved.startDate).toBe('2026-07-15')
    expect(moved.endDate).toBe('2026-07-24')
  })
})
