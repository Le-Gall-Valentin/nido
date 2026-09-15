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

/**
 * Exactly 42 dates: six Monday-started weeks covering the anchor's month.
 *
 * Always six rows, never five, so the grid's height does not jump between months — a shifting
 * layout under the thumb is what makes a month view feel unstable.
 */
export function monthGridDates(iso: string): string[] {
  const first = startOfWeek(startOfMonth(iso))
  return Array.from({ length: 42 }, (_, i) => addDays(first, i))
}

/** The [from, to] the feed must be asked for — the whole visible grid, not just the month. */
export function windowFor(view: CalendarView, iso: string): { from: string; to: string } {
  if (view === 'day') return { from: iso, to: iso }
  if (view === 'week') {
    const monday = startOfWeek(iso)
    return { from: monday, to: addDays(monday, 6) }
  }
  const first = startOfWeek(startOfMonth(iso))
  return { from: first, to: addDays(first, 41) }
}

/**
 * Groups occurrences by every ISO day they occupy — a multi-day one appears on each day it spans,
 * not only on its first. A naive group-by-start puts a fortnight's holiday on July 1 and nowhere
 * else, which is the bug this function exists to prevent.
 */
export function groupByDay(
  occurrences: CalendarOccurrence[], days: string[],
): Map<string, CalendarOccurrence[]> {
  const inWindow = new Set(days)
  const grouped = new Map<string, CalendarOccurrence[]>()
  for (const occurrence of occurrences) {
    const span = Math.max(0, daysBetween(occurrence.startDate, occurrence.endDate))
    for (let i = 0; i <= span; i++) {
      const day = addDays(occurrence.startDate, i)
      if (!inWindow.has(day)) continue
      const bucket = grouped.get(day)
      if (bucket) bucket.push(occurrence)
      else grouped.set(day, [occurrence])
    }
  }
  return grouped
}
