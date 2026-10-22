import type { EventInput } from '@/entities/calendar'
import { isValidIso } from './calendarWindow'
import { MAX_EVENT_DAYS, MAX_INTERVAL_COUNT, coversTooManyDays, occurrenceOutlastsInterval } from './scheduleLimits'
import type { Recurrence } from './seriesInput'

/** Why the event form cannot be saved: the message's key, and the values it says. */
export interface EventFormProblem {
  key: string
  values?: Record<string, unknown>
}

/** An interval over the server's limit: what the form says, whether it saw it first or the server refused it. */
export const intervalTooLarge: EventFormProblem = { key: 'series.interval_too_large', values: { maximum: MAX_INTERVAL_COUNT } }

/**
 * The first reason, in the order the form reads, why an event cannot be saved — a series when `recurrence` is given,
 * as it is sent; null when it can. The server's limits are among them, so that the form says why rather than reports
 * a failed save.
 */
export function eventFormProblem(event: EventInput, recurrence: Recurrence | null): EventFormProblem | null {
  if (!event.title.trim()) return { key: 'form.title_required' }
  if (!isValidIso(event.startDate) || !isValidIso(event.endDate)) return { key: 'form.dates_required' }
  if (event.endDate < event.startDate) return { key: 'form.end_before_start' }
  if (!event.allDay && (!event.startTime || !event.endTime)) return { key: 'form.times_required' }
  if (!event.allDay && event.startDate === event.endDate
      && event.endTime && event.startTime && event.endTime < event.startTime) {
    // Only comparable on a single day — across days an "earlier" end time is an overnight event.
    return { key: 'form.end_before_start' }
  }
  if (coversTooManyDays(event)) return { key: 'form.too_long', values: { days: MAX_EVENT_DAYS } }
  if (!recurrence) return null
  if (!(recurrence.intervalCount >= 1)) return { key: 'series.interval_invalid' }
  // The server reads an interval as a whole number: 1.5 would be saved as 1, after the checks below ran on 1.5.
  if (!Number.isInteger(recurrence.intervalCount)) return { key: 'series.interval_not_whole' }
  if (recurrence.intervalCount > MAX_INTERVAL_COUNT) return intervalTooLarge
  if (occurrenceOutlastsInterval(event, recurrence)) return { key: 'series.occurrence_too_long' }
  if (recurrence.until && recurrence.until < event.startDate) return { key: 'series.until_before_start' }
  return null
}
