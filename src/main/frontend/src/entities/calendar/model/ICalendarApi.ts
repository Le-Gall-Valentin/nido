import type { CalendarEvent, CalendarOccurrence, EventInput } from './types'

/**
 * Port for the feed and for the calendar's own events. Recurring series management lives in
 * IRecurringEventSeriesApi instead — a hook that only reads the feed shouldn't need to know
 * this interface's write half exists.
 */
export interface ICalendarApi {
  /** One request per window, whatever the number of sources behind it. */
  listOccurrences(spaceId: string, from: string, to: string): Promise<CalendarOccurrence[]>
  createEvent(spaceId: string, input: EventInput): Promise<CalendarEvent>
  updateEvent(spaceId: string, eventId: string, input: EventInput): Promise<CalendarEvent>
  deleteEvent(spaceId: string, eventId: string): Promise<void>
  joinEvent(spaceId: string, eventId: string): Promise<void>
  leaveEvent(spaceId: string, eventId: string): Promise<void>
  copyEvent(spaceId: string, eventId: string, destinationSpaceId: string): Promise<CalendarEvent>
  moveEvent(spaceId: string, eventId: string, destinationSpaceId: string): Promise<CalendarEvent>
}
