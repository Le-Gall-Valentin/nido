import type { EventInput, RecurrenceInterval, RecurringEventSeries, RecurringEventSeriesInput } from '@/entities/calendar'
import { addDays, daysBetween } from './calendarWindow'

/** How an event repeats: what the form's recurrence box adds to an event. */
export interface Recurrence {
  intervalType: RecurrenceInterval
  intervalCount: number
  /** The last day an occurrence may start, or null for a series that never ends. */
  until: string | null
}

/**
 * A series is an event that repeats: its first occurrence is the event, and every other one lasts
 * as long. The event's end is kept as the number of days past its start, which is how a series
 * says that each of its occurrences runs from a Friday evening into the Saturday.
 *
 * `edited` is the series being edited, if any. Its start shown is its first occurrence, which is not
 * its anchor once it carries on an edit: left alone, that start keeps the series' own rhythm.
 */
export function toSeriesInput(event: EventInput, recurrence: Recurrence, edited?: RecurringEventSeries | null): RecurringEventSeriesInput {
  const anchorDate = edited && event.startDate === shownStart(edited) ? edited.anchorDate : event.startDate
  return {
    title: event.title,
    description: event.description,
    location: event.location,
    allDay: event.allDay,
    startTime: event.startTime,
    endTime: event.endTime,
    durationDays: daysBetween(event.startDate, event.endDate),
    color: event.color,
    intervalType: recurrence.intervalType,
    intervalCount: recurrence.intervalCount,
    anchorDate,
    endDate: recurrence.until,
    participantIds: event.participantIds,
  }
}

/** Where a series is shown from: its first occurrence, or its anchor when it has none left. */
function shownStart(series: RecurringEventSeries): string {
  return series.firstDate ?? series.anchorDate
}

/**
 * Whether a series lies on both sides of `today` — begun before it and not over. Editing or deleting
 * one leaves its past as it was, as the server does.
 */
export function runsAcross(series: RecurringEventSeries, today: string): boolean {
  return series.firstDate !== null && series.firstDate < today && (series.endDate === null || series.endDate >= today)
}

/** A series as the event form shows it: its first occurrence shown, and how it repeats. */
export function fromSeries(series: RecurringEventSeries): { event: EventInput; recurrence: Recurrence } {
  const startDate = shownStart(series)
  return {
    event: {
      title: series.title,
      description: series.description,
      location: series.location,
      allDay: series.allDay,
      startDate,
      startTime: series.startTime,
      endDate: addDays(startDate, series.durationDays),
      endTime: series.endTime,
      color: series.color,
      participantIds: series.participantIds,
    },
    recurrence: { intervalType: series.intervalType, intervalCount: series.intervalCount, until: series.endDate },
  }
}
