import { daysBetween } from './calendarWindow'
import { dateTimeAt } from './dragResolution'
import type { ScheduleChange } from '@/entities/calendar'
import { DAY_MINUTES, SNAP_MINUTES } from './timeMath'

/** A place in the hour grid: a day, and the unrounded time under the pointer on it. */
export interface GridPoint { day: string; minutes: number }

/** What a click on an empty slot creates, like a paper diary's default line. */
const CLICK_MINUTES = 60

/** Minutes from the anchor day's midnight to the start of the quarter hour a point falls in. */
function slotStart(point: GridPoint, anchorDay: string): number {
  const slot = Math.min(Math.floor(point.minutes / SNAP_MINUTES) * SNAP_MINUTES, DAY_MINUTES - SNAP_MINUTES)
  return daysBetween(anchorDay, point.day) * DAY_MINUTES + slot
}

/**
 * The time picked out by a press and a release in the hour grid: every quarter hour between the
 * two, whichever way the drag went and across as many days as it crossed. A press and release in
 * the same quarter hour is a click, and gives an hour from there.
 */
export function selectionRange(anchor: GridPoint, current: GridPoint): ScheduleChange {
  const a = slotStart(anchor, anchor.day)
  const c = slotStart(current, anchor.day)
  const start = Math.min(a, c)
  const end = a === c ? start + CLICK_MINUTES : Math.max(a, c) + SNAP_MINUTES
  const from = dateTimeAt(anchor.day, start)
  const to = dateTimeAt(anchor.day, end)
  return { allDay: false, startDate: from.date, startTime: from.time, endDate: to.date, endTime: to.time }
}

/** The days picked out in the all-day band: an all-day event over them, whichever way the drag went. */
export function bandRange(anchorDay: string, currentDay: string): ScheduleChange {
  const [startDate, endDate] = anchorDay <= currentDay ? [anchorDay, currentDay] : [currentDay, anchorDay]
  return { allDay: true, startDate, startTime: null, endDate, endTime: null }
}
