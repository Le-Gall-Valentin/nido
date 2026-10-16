import type { EventInput } from '@/entities/calendar'
import { daysBetween } from './calendarWindow'
import { shortestIntervalDays } from './recurrenceRule'
import type { Recurrence } from './seriesInput'
import { DAY_MINUTES, timeToMinutes } from './timeMath'

/**
 * The most days an event may cover — the server's own limit, checked here so the form can say why
 * rather than report a failed save.
 */
export const MAX_EVENT_DAYS = 366

export function coversTooManyDays(event: EventInput): boolean {
  return daysBetween(event.startDate, event.endDate) + 1 > MAX_EVENT_DAYS
}

/**
 * Whether each occurrence would run into the next one — the server refuses it. The next one is the nearest
 * between the two closest days a series repeats on, a month at its shortest, a year likewise. An all-day
 * occurrence is measured in the days it covers, a timed one to the minute, so a nightly shift from
 * 22:00 to 02:00 fits a daily series.
 */
export function occurrenceOutlastsInterval(event: EventInput, recurrence: Recurrence): boolean {
  const intervalDays = shortestIntervalDays(recurrence)
  const spanDays = daysBetween(event.startDate, event.endDate)
  if (event.allDay || !event.startTime || !event.endTime) return spanDays + 1 > intervalDays
  const minutes = spanDays * DAY_MINUTES + timeToMinutes(event.endTime) - timeToMinutes(event.startTime)
  return minutes > intervalDays * DAY_MINUTES
}
