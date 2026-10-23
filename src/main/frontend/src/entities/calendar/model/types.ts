import type { EventColor } from '../lib/eventPalette'

export type CalendarSourceType = 'EVENT' | 'TASK' | 'FINANCE' | 'MEAL' | 'SAVINGS' | 'EXTERNAL'

export type RecurrenceInterval = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

/** Which day of a month a monthly or yearly series falls on, read off its anchor — the server's DayRule. */
export type DayRule = 'SAME_DAY' | 'WEEKDAY_OF_MONTH' | 'LAST_WEEKDAY_OF_MONTH' | 'LAST_DAY_OF_MONTH'

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
  /** The followed calendar an EXTERNAL entry comes from; null for every other source. */
  externalFeedId: string | null
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
  /** The days a weekly series repeats on, its anchor's among them; null for any other frequency. */
  weekdays: Weekday[] | null
  dayRule: DayRule
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
  /** The days a weekly series repeats on, its anchor's among them; null for any other frequency. */
  weekdays: Weekday[] | null
  dayRule: DayRule
  anchorDate: string
  endDate: string | null
  participantIds: string[]
}

/** When something runs: what a drag, a resize or a selection in the grid decides, before it is written. */
export interface ScheduleChange {
  allDay: boolean; startDate: string; startTime: string | null; endDate: string; endTime: string | null
}

/** What an export or a subscription may write: every source nido owns, a followed calendar never. */
export type ExportableSource = Exclude<CalendarSourceType, 'EXTERNAL'>

export interface SubscriptionLink {
  id: string
  label: string
  sources: ExportableSource[]
  onlyMine: boolean
  createdAt: string
  /** When a calendar application last read it; absent until it has. */
  lastReadAt?: string | null
}

export interface SubscriptionLinkInput {
  label: string
  sources: ExportableSource[]
  onlyMine: boolean
}

/** The one answer holding the address: it is shown once and kept nowhere. */
export interface CreatedSubscriptionLink {
  link: SubscriptionLink
  url: string
  webcalUrl: string
}

export type ImportNoteKind = 'UNFOLDED' | 'UNFOLDED_PARTIAL' | 'TRUNCATED_TEXT' | 'CONVERTED_ZONE' | 'UNKNOWN_ZONE' | 'IGNORED'

export interface ImportNote {
  kind: ImportNoteKind
  title: string | null
  detail: string | null
}

/** The most notes the server puts in one report: past it, the others are left out. */
export const MAX_IMPORT_NOTES = 200

/** The largest file the server imports (IcalUpload): 5 MB of 1024 × 1024 bytes. */
export const MAX_IMPORT_BYTES = 5 * 1024 * 1024

/** What an import did — or, `written` false, what it would do. */
export interface ImportReport {
  eventsCreated: number
  seriesCreated: number
  seriesUnfolded: number
  occurrencesUnfolded: number
  duplicates: number
  ignored: number
  notes: ImportNote[]
  written: boolean
}

/** A calendar the space follows: its host, never its address. */
export interface ExternalFeed {
  id: string
  name: string
  host: string
  color: string | null
  contentFetchedAt: string
  lastAttemptAt: string
  /** "TIMEOUT", "HTTP_404"… — why the last attempt failed; null when it did not. */
  lastError: string | null
}

/** The longest address of a calendar to follow the server takes (ExternalFeedRequest). */
export const MAX_FEED_ADDRESS_LENGTH = 2000

/** A calendar to follow. A colour is one of the palette, or none for the graphite of external sources. */
export interface ExternalFeedInput {
  url: string
  name: string | null
  color: EventColor | null
}

/** What changes of a followed calendar: its name and its colour, never its address. */
export interface ExternalFeedUpdate {
  name: string
  color: EventColor | null
}
