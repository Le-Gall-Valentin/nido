import type { CalendarOccurrence } from '@/entities/calendar'
import { addDays, daysBetween } from './calendarWindow'
import { DAY_MINUTES, timeToMinutes } from './timeMath'

export interface Segment { startMinutes: number; endMinutes: number; isStart: boolean; isEnd: boolean }

function endsAtMidnight(o: CalendarOccurrence): boolean {
  return !o.allDay && o.endTime !== null && timeToMinutes(o.endTime) === 0 && o.endDate > o.startDate
}

/** The last day an occurrence actually occupies. An end at 00:00 belongs to the day before. */
export function effectiveEndDate(o: CalendarOccurrence): string {
  return endsAtMidnight(o) ? addDays(o.endDate, -1) : o.endDate
}

/** Whether an occurrence is shown on `day` — the same days the month, the band and the lists use. */
export function covers(o: CalendarOccurrence, day: string): boolean {
  return day >= o.startDate && day <= effectiveEndDate(o)
}

function totalMinutes(o: CalendarOccurrence): number {
  if (o.allDay || !o.startTime || !o.endTime) return 0
  return daysBetween(o.startDate, o.endDate) * DAY_MINUTES + timeToMinutes(o.endTime) - timeToMinutes(o.startTime)
}

/**
 * Whether an occurrence belongs in the all-day band rather than the hour grid: everything without a
 * time, and any timed event longer than a day, which would otherwise fill whole columns.
 */
export function isBandOccurrence(o: CalendarOccurrence): boolean {
  return o.allDay || totalMinutes(o) > DAY_MINUTES
}

/**
 * The piece of a timed occurrence drawn on one day. A 22:00 → 02:00 evening is 22:00 → 24:00 on its
 * first day and 00:00 → 02:00 on the next. Every day's copy used to be drawn from the event's own
 * start and end, which put that second piece at 22:00 with the minimum height.
 */
export function segmentFor(o: CalendarOccurrence, day: string): Segment | null {
  if (isBandOccurrence(o) || !o.startTime || !o.endTime) return null
  const last = effectiveEndDate(o)
  if (day < o.startDate || day > last) return null
  const isStart = day === o.startDate
  const isEnd = day === last
  const startMinutes = isStart ? timeToMinutes(o.startTime) : 0
  const endMinutes = isEnd && !endsAtMidnight(o) ? timeToMinutes(o.endTime) : DAY_MINUTES
  return { startMinutes, endMinutes, isStart, isEnd }
}
