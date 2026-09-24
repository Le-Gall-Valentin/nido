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
 */
export function toSeriesInput(event: EventInput, recurrence: Recurrence): RecurringEventSeriesInput {
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
    anchorDate: event.startDate,
    endDate: recurrence.until,
    participantIds: event.participantIds,
  }
}

/** A series as the event form shows it: its first occurrence, and how it repeats. */
export function fromSeries(series: RecurringEventSeries): { event: EventInput; recurrence: Recurrence } {
  return {
    event: {
      title: series.title,
      description: series.description,
      location: series.location,
      allDay: series.allDay,
      startDate: series.anchorDate,
      startTime: series.startTime,
      endDate: addDays(series.anchorDate, series.durationDays),
      endTime: series.endTime,
      color: series.color,
      participantIds: series.participantIds,
    },
    recurrence: { intervalType: series.intervalType, intervalCount: series.intervalCount, until: series.endDate },
  }
}
