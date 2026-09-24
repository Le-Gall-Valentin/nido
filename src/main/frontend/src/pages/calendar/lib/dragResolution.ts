import type { CalendarOccurrence } from '@/entities/calendar'
import { addDays, daysBetween } from './calendarWindow'
import { effectiveEndDate } from './segments'
import { DAY_MINUTES, minutesToTime, timeToMinutes } from './timeMath'
import type { DragIntent, DropTarget, ScheduleChange } from './dragTypes'

export const MIN_DURATION_MINUTES = 15
const ONE_HOUR = 60

function scheduleOf(o: CalendarOccurrence): ScheduleChange {
  return {
    allDay: o.allDay,
    startDate: o.startDate, startTime: o.startTime ? minutesToTime(timeToMinutes(o.startTime)) : null,
    endDate: o.endDate, endTime: o.endTime ? minutesToTime(timeToMinutes(o.endTime)) : null,
  }
}

function same(a: ScheduleChange, b: ScheduleChange): boolean {
  return a.allDay === b.allDay && a.startDate === b.startDate && a.startTime === b.startTime
    && a.endDate === b.endDate && a.endTime === b.endTime
}

/** Minutes from the start day's midnight to the given date and time — overnight ends exceed 1440. */
function absolute(startDate: string, date: string, time: string): number {
  return daysBetween(startDate, date) * DAY_MINUTES + timeToMinutes(time)
}

/**
 * A date and time from minutes past `day`'s midnight, either way: exactly 24:00 reads as the next
 * day at 00:00, and -60 as the day before at 23:00.
 */
function at(day: string, minutes: number): { date: string; time: string } {
  const rest = ((minutes % DAY_MINUTES) + DAY_MINUTES) % DAY_MINUTES
  return { date: addDays(day, Math.floor(minutes / DAY_MINUTES)), time: minutesToTime(rest) }
}

/** The same schedule, `days` later (or earlier). */
function shiftDays(o: CalendarOccurrence, days: number): ScheduleChange {
  return { ...scheduleOf(o), startDate: addDays(o.startDate, days), endDate: addDays(o.endDate, days) }
}

/** How many days separate the grabbed piece from the day it was dropped on. */
function dayShift(intent: DragIntent, target: DropTarget): number {
  return daysBetween(intent.kind === 'move' ? intent.day : intent.occurrence.startDate, target.day)
}

function timedFrom(day: string, startMinutes: number, duration: number): ScheduleChange {
  const start = at(day, startMinutes)
  const end = at(day, startMinutes + duration)
  return { allDay: false, startDate: start.date, startTime: start.time, endDate: end.date, endTime: end.time }
}

/**
 * What a drop means, as a new schedule — or null when it means nothing (put back where it was, or a
 * grid drop with no pointer time). The only place drag rules live; the layer measures, this decides.
 */
export function resolveDrop(intent: DragIntent, target: DropTarget, pointerMinutes: number | null): ScheduleChange | null {
  const next = decide(intent, target, pointerMinutes)
  return next === null || isUnchanged(intent.occurrence, next) ? null : next
}

/**
 * Where the item is drawn while it is dragged: the same rules as the drop, but an unchanged schedule
 * is still a place — hovering the item's own slot shows it there rather than nowhere.
 */
export function previewDrop(intent: DragIntent, target: DropTarget, pointerMinutes: number | null): ScheduleChange | null {
  return decide(intent, target, pointerMinutes)
}

/** Whether two landing slots are the same one — including "no slot" on both sides. */
export function sameSchedule(a: ScheduleChange | null, b: ScheduleChange | null): boolean {
  return a === null || b === null ? a === b : same(a, b)
}

/** Whether a schedule is the one the occurrence already has — the API's "14:00:00" is the drag's "14:00". */
export function isUnchanged(o: CalendarOccurrence, change: ScheduleChange): boolean {
  return same(scheduleOf(o), change)
}

function decide(intent: DragIntent, target: DropTarget, pointerMinutes: number | null): ScheduleChange | null {
  const o = intent.occurrence

  // Tasks and meals have no time and never convert: any target only says which day.
  if (o.source !== 'EVENT') return shiftDays(o, dayShift(intent, target))

  if (intent.kind === 'move') {
    const days = dayShift(intent, target)
    if (target.kind === 'day') return shiftDays(o, days)
    if (target.kind === 'all-day') {
      if (intent.from !== 'grid' || o.allDay) return shiftDays(o, days)
      // An end at 00:00 belongs to the day before: a 22:00 → 00:00 evening is a one-day event.
      const startDate = addDays(o.startDate, days)
      const span = daysBetween(o.startDate, effectiveEndDate(o))
      return { allDay: true, startDate, startTime: null, endDate: addDays(startDate, span), endTime: null }
    }
    if (pointerMinutes === null) return null
    if (o.allDay || !o.startTime || !o.endTime) {
      return timedFrom(target.day, Math.min(pointerMinutes, DAY_MINUTES - MIN_DURATION_MINUTES), ONE_HOUR)
    }
    const startAbs = timeToMinutes(o.startTime)
    const duration = absolute(o.startDate, o.endDate, o.endTime) - startAbs
    if (intent.grabMinutes === undefined) {
      // From the band (a timed event longer than a day): it starts where it is dropped.
      return timedFrom(target.day, Math.min(pointerMinutes, DAY_MINUTES - MIN_DURATION_MINUTES), duration)
    }
    // From the grid: moved by exactly as far as the pointer travelled, so a start off the quarter
    // hour keeps its minutes, and a block put back has not moved.
    const moved = days * DAY_MINUTES + pointerMinutes - intent.grabMinutes
    return timedFrom(o.startDate, startAbs + moved, duration)
  }

  // Resizing: only timed events carry handles.
  if (o.allDay || !o.startTime || !o.endTime || pointerMinutes === null) return null
  const startAbs = timeToMinutes(o.startTime)
  const endAbs = absolute(o.startDate, o.endDate, o.endTime)

  if (intent.kind === 'resize-start') {
    const newStart = Math.min(pointerMinutes, endAbs - MIN_DURATION_MINUTES)
    const start = at(o.startDate, Math.max(0, newStart))
    return { ...scheduleOf(o), startTime: start.time, startDate: start.date }
  }

  // resize-end: the handle lives on the last piece, so the pointer is read against that day.
  const dayOffset = daysBetween(o.startDate, target.day) * DAY_MINUTES
  const newEnd = Math.max(dayOffset + pointerMinutes, startAbs + MIN_DURATION_MINUTES)
  const end = at(o.startDate, newEnd)
  return { ...scheduleOf(o), endDate: end.date, endTime: end.time }
}
