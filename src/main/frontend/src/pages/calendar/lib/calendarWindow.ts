import type { CalendarOccurrence } from '@/entities/calendar'

export type CalendarView = 'month' | 'week' | 'day'

/** Milliseconds in a day. Every computation here is UTC-anchored so no DST shift can move a date. */
const DAY_MS = 86_400_000

function parse(iso: string): Date {
  return new Date(`${iso}T00:00:00Z`)
}

export function toIso(date: Date): string {
  return date.toISOString().slice(0, 10)
}

export function addDays(iso: string, days: number): string {
  return toIso(new Date(parse(iso).getTime() + days * DAY_MS))
}

export function daysBetween(fromIso: string, toIso_: string): number {
  return Math.round((parse(toIso_).getTime() - parse(fromIso).getTime()) / DAY_MS)
}

/**
 * True only for a real calendar date. A round-trip rather than a regex, because `2026-02-30`
 * matches any plausible pattern and still is not a day — and a hand-edited URL must never be
 * able to crash the page.
 */
export function isValidIso(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const date = parse(value)
  return !Number.isNaN(date.getTime()) && toIso(date) === value
}

/** The Monday on or before `iso`. Weeks start on Monday here, as they do in French calendars. */
export function startOfWeek(iso: string): string {
  const day = parse(iso).getUTCDay()
  return addDays(iso, -((day + 6) % 7))
}

export function startOfMonth(iso: string): string {
  return `${iso.slice(0, 7)}-01`
}

/** Clamps to the target month's length, so Jan 31 + 1 month is Feb 28 and never spills into March. */
export function addMonths(iso: string, months: number): string {
  const date = parse(iso)
  const target = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + months, 1))
  const lastDay = new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth() + 1, 0)).getUTCDate()
  const day = Math.min(date.getUTCDate(), lastDay)
  return toIso(new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth(), day)))
}

/** Seven ISO dates, Monday first. */
export function weekDates(iso: string): string[] {
  const monday = startOfWeek(iso)
  return Array.from({ length: 7 }, (_, i) => addDays(monday, i))
}

/** From the Monday of the week holding the month's first day to the Sunday of the one holding its last. */
function monthGridSpan(iso: string): { from: string; to: string } {
  const first = startOfMonth(iso)
  const last = addDays(addMonths(first, 1), -1)
  return { from: startOfWeek(first), to: addDays(startOfWeek(last), 6) }
}

/**
 * The Monday-started weeks the anchor's month touches: four, five or six of them.
 *
 * A week made only of the next month is left out — it shows nothing of the month being read, and
 * padding every month to six weeks ran September on to October 11. The grid's height follows the
 * month instead of staying the same.
 */
export function monthGridDates(iso: string): string[] {
  const { from, to } = monthGridSpan(iso)
  return Array.from({ length: daysBetween(from, to) + 1 }, (_, i) => addDays(from, i))
}

/** The [from, to] the feed must be asked for — the whole visible grid, not just the month. */
export function windowFor(view: CalendarView, iso: string): { from: string; to: string } {
  if (view === 'day') return { from: iso, to: iso }
  if (view === 'week') {
    const monday = startOfWeek(iso)
    return { from: monday, to: addDays(monday, 6) }
  }
  return monthGridSpan(iso)
}

/**
 * Groups occurrences by every ISO day they occupy — a multi-day one appears on each day it spans,
 * not only on its first. A naive group-by-start puts a fortnight's holiday on July 1 and nowhere
 * else, which is the bug this function exists to prevent.
 *
 * It walks the days shown, not the days an occurrence lasts: a month costs at most 42 steps per
 * occurrence however long that occurrence runs.
 */
export function groupByDay(
  occurrences: CalendarOccurrence[], days: string[],
): Map<string, CalendarOccurrence[]> {
  const grouped = new Map<string, CalendarOccurrence[]>()
  for (const occurrence of occurrences) {
    // An end at exactly 00:00 belongs to the day before — the same rule as segments.ts, inlined
    // here because segments.ts depends on this module.
    const endsAtMidnight = !occurrence.allDay && occurrence.endTime !== null
      && occurrence.endTime.startsWith('00:00') && occurrence.endDate > occurrence.startDate
    const lastDay = endsAtMidnight ? addDays(occurrence.endDate, -1) : occurrence.endDate
    for (const day of days) {
      // ISO days compare as strings. An occurrence ending before it starts still shows on its first day.
      if (day < occurrence.startDate || (day > lastDay && day !== occurrence.startDate)) continue
      const bucket = grouped.get(day)
      if (bucket) bucket.push(occurrence)
      else grouped.set(day, [occurrence])
    }
  }
  return grouped
}
