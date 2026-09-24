import type { CalendarOccurrence } from '@/entities/calendar'
import { addDays, daysBetween } from './calendarWindow'
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

/** A date and time from minutes past `day`'s midnight; exactly 24:00 reads as the next day at 00:00. */
function at(day: string, minutes: number): { date: string; time: string } {
  return { date: addDays(day, Math.floor(minutes / DAY_MINUTES)), time: minutesToTime(minutes % DAY_MINUTES) }
}

function shiftDays(o: CalendarOccurrence, day: string): ScheduleChange {
  const current = scheduleOf(o)
  return { ...current, startDate: day, endDate: addDays(day, daysBetween(o.startDate, o.endDate)) }
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
  const o = intent.occurrence
  const next = decide(intent, target, pointerMinutes)
  return next === null || same(scheduleOf(o), next) ? null : next
}

function decide(intent: DragIntent, target: DropTarget, pointerMinutes: number | null): ScheduleChange | null {
  const o = intent.occurrence

  // Tasks and meals have no time and never convert: any target only says which day.
  if (o.source !== 'EVENT') return shiftDays(o, target.day)

  if (intent.kind === 'move') {
    if (target.kind === 'day') return shiftDays(o, target.day)
    if (target.kind === 'all-day') {
      if (intent.from !== 'grid' || o.allDay) return shiftDays(o, target.day)
      const span = daysBetween(o.startDate, o.endDate)
      return { allDay: true, startDate: target.day, startTime: null, endDate: addDays(target.day, span), endTime: null }
    }
    if (pointerMinutes === null) return null
    const start = Math.min(pointerMinutes, DAY_MINUTES - MIN_DURATION_MINUTES)
    if (o.allDay || !o.startTime || !o.endTime) return timedFrom(target.day, start, ONE_HOUR)
    const duration = absolute(o.startDate, o.endDate, o.endTime) - timeToMinutes(o.startTime)
    return timedFrom(target.day, start, duration)
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
