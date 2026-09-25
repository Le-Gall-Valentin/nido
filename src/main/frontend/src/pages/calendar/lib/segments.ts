import type { CalendarOccurrence } from '@/entities/calendar'
import { addDays } from './calendarWindow'
import { DAY_MINUTES, timeToMinutes } from './timeMath'

export interface Segment { startMinutes: number; endMinutes: number; isStart: boolean; isEnd: boolean }

/** Just the when of an occurrence — also what a drop or a picked-out time produces. */
export type Schedule = Pick<CalendarOccurrence, 'allDay' | 'startDate' | 'startTime' | 'endDate' | 'endTime'>

function endsAtMidnight(o: Schedule): boolean {
  return !o.allDay && o.endTime !== null && timeToMinutes(o.endTime) === 0 && o.endDate > o.startDate
}

/** The last day an occurrence actually occupies. An end at 00:00 belongs to the day before. */
export function effectiveEndDate(o: Schedule): string {
  return endsAtMidnight(o) ? addDays(o.endDate, -1) : o.endDate
}

/** Whether an occurrence is shown on `day` — the same days the month, the band and the lists use. */
export function covers(o: Schedule, day: string): boolean {
  return day >= o.startDate && day <= effectiveEndDate(o)
}

/**
 * Whether an occurrence belongs in the all-day band rather than the hour grid: exactly what has no
 * time. A timed event lasting several days stays in the grid, one piece per day — it has hours,
 * and the band is where "all day" goes.
 */
export function isBandOccurrence(o: Schedule): boolean {
  return o.allDay
}

/**
 * The piece of a timed occurrence drawn on one day. A 22:00 → 02:00 evening is 22:00 → 24:00 on its
 * first day and 00:00 → 02:00 on the next. Every day's copy used to be drawn from the event's own
 * start and end, which put that second piece at 22:00 with the minimum height.
 */
export function segmentFor(o: Schedule, day: string): Segment | null {
  if (isBandOccurrence(o) || !o.startTime || !o.endTime) return null
  const last = effectiveEndDate(o)
  if (day < o.startDate || day > last) return null
  const isStart = day === o.startDate
  const isEnd = day === last
  const startMinutes = isStart ? timeToMinutes(o.startTime) : 0
  const endMinutes = isEnd && !endsAtMidnight(o) ? timeToMinutes(o.endTime) : DAY_MINUTES
  return { startMinutes, endMinutes, isStart, isEnd }
}
