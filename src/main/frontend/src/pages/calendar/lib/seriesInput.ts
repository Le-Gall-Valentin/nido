import type { DayRule, EventInput, RecurrenceInterval, RecurringEventSeries, RecurringEventSeriesInput, Weekday } from '@/entities/calendar'
import { addDays, daysBetween } from './calendarWindow'

/** How an event repeats: what the form's recurrence box adds to an event. */
export interface Recurrence {
  intervalType: RecurrenceInterval
  intervalCount: number
  /** The last day an occurrence may start, or null for a series that never ends. */
  until: string | null
  /** The days a weekly series repeats on; ignored for any other frequency. */
  weekdays: Weekday[]
  /** Which day of a month a monthly or yearly series falls on; ignored otherwise. */
  dayRule: DayRule
}

const monthlyOrYearly = (type: RecurrenceInterval) => type === 'MONTHLY' || type === 'YEARLY'

function sameDays(a: readonly Weekday[] | null, b: readonly Weekday[] | null): boolean {
  if (a === null || b === null) return a === b
  return a.length === b.length && a.every((day) => b.includes(day))
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
  const weekdays = recurrence.intervalType === 'WEEKLY' ? recurrence.weekdays : null
  const dayRule: DayRule = monthlyOrYearly(recurrence.intervalType) ? recurrence.dayRule : 'SAME_DAY'
  // The anchor sets the rhythm and the days are read off it: it is kept only while the start and the days are left
  // alone. Otherwise the series is anchored on the start shown — one of its occurrences, in a week of its rhythm.
  const keepsItsDays = !!edited && sameDays(edited.weekdays ?? null, weekdays) && (edited.dayRule ?? 'SAME_DAY') === dayRule
  const anchorDate = edited && keepsItsDays && event.startDate === shownStart(edited) ? edited.anchorDate : event.startDate
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
    weekdays,
    dayRule,
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

/**
 * A series as the event form shows it: its first occurrence shown, and how it repeats. A series that names no days
 * repeats on its start's alone, which the form always adds.
 */
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
    recurrence: {
      intervalType: series.intervalType,
      intervalCount: series.intervalCount,
      until: series.endDate,
      weekdays: series.weekdays ?? [],
      dayRule: series.dayRule ?? 'SAME_DAY',
    },
  }
}
