import { describe, it, expect } from 'vitest'
import { toEventInput } from './toEventInput'
import type { CalendarOccurrence } from '../model/types'

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
