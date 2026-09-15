export type CalendarSourceType = 'EVENT' | 'TASK' | 'FINANCE' | 'MEAL' | 'SAVINGS'

export type RecurrenceInterval = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

/**
 * One entry of the unified feed, whatever produced it.
 *
 * `sourceId` is a string, not a UUID: a projected occurrence has no row and therefore no id of
 * its own — it is keyed by its series and slot as `"<seriesId>:<date>"`. Treat it as opaque and
 * never parse it.
 *
 * `materialized` false means nothing in the database corresponds to this entry. That is what
 * tells the UI that deleting it excludes a slot rather than deleting a row, and that it cannot
 * simply be dragged to another date.
 */
export interface CalendarOccurrence {
  source: CalendarSourceType
  sourceId: string
  seriesId: string | null
  originalDate: string | null
  materialized: boolean
  title: string
  allDay: boolean
  startDate: string
  startTime: string | null
  endDate: string
  endTime: string | null
  color: string | null
  participantIds: string[]
}

export interface CalendarEvent {
  id: string
  title: string
  description: string | null
  location: string | null
  allDay: boolean
  startDate: string
  startTime: string | null
  endDate: string
  endTime: string | null
  color: string | null
  participantIds: string[]
  recurringSeriesId: string | null
  /** The slot this event replaces — never where it now sits. */
  recurringOriginalDate: string | null
  createdBy: string
  createdAt: string
}

export interface RecurringEventSeries {
  id: string
  title: string
  description: string | null
  location: string | null
  allDay: boolean
  startTime: string | null
  endTime: string | null
  /** Days past its start that each occurrence runs; 0 for a same-day event. */
  durationDays: number
  color: string | null
  intervalType: RecurrenceInterval
  intervalCount: number
  anchorDate: string
  endDate: string | null
  participantIds: string[]
  createdBy: string
  createdAt: string
}

/** The writable half of an event, shared by creation, edition and per-occurrence overrides. */
export interface EventInput {
  title: string
  description: string | null
  location: string | null
  allDay: boolean
  startDate: string
  startTime: string | null
  endDate: string
  endTime: string | null
  color: string | null
  participantIds: string[]
}

export interface RecurringEventSeriesInput {
  title: string
  description: string | null
  location: string | null
  allDay: boolean
  startTime: string | null
  endTime: string | null
  durationDays: number
  color: string | null
  intervalType: RecurrenceInterval
  intervalCount: number
  anchorDate: string
  endDate: string | null
  participantIds: string[]
}
