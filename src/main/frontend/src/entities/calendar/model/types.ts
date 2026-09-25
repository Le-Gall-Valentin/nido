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
  /** Null for sources with no such notion (tasks, finance, meals, savings). */
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
  /** Sets the rhythm. The first occurrence shown is `firstDate`, later once the series carries on an edit. */
  anchorDate: string
  endDate: string | null
  /** Set when the series carries on an edit made after the series it replaces began: it shows from then. */
  startsOn: string | null
  /** Its first occurrence shown, or null when it has none left. */
  firstDate: string | null
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

/** When something runs: what a drag, a resize or a selection in the grid decides, before it is written. */
export interface ScheduleChange {
  allDay: boolean; startDate: string; startTime: string | null; endDate: string; endTime: string | null
}
