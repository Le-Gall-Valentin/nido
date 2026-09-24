import type { CalendarOccurrence, EventInput } from '@/entities/calendar'
import { addDays, daysBetween } from './calendarWindow'

/**
 * The writable half of an occurrence, field for field.
 *
 * Every write that starts from something already on screen goes through here — the edit form and
 * drag-to-reschedule alike — so there is exactly one place where a field can be forgotten, and a
 * test that says which fields must survive. Forgetting description and location is how opening an
 * event and saving it untouched used to erase both.
 */
export function toEventInput(occurrence: CalendarOccurrence): EventInput {
  return {
    title: occurrence.title,
    description: occurrence.description,
    location: occurrence.location,
    allDay: occurrence.allDay,
    startDate: occurrence.startDate,
    startTime: occurrence.startTime,
    endDate: occurrence.endDate,
    endTime: occurrence.endTime,
    color: occurrence.color,
    participantIds: occurrence.participantIds,
  }
}

/** The same event on another day. A multi-day event keeps its length: dragging moves, never resizes. */
export function rescheduledInput(occurrence: CalendarOccurrence, targetDay: string): EventInput {
  const span = daysBetween(occurrence.startDate, occurrence.endDate)
  return { ...toEventInput(occurrence), startDate: targetDay, endDate: addDays(targetDay, span) }
}
