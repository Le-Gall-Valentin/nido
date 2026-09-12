/**
 * How many occurrences a recurring series has already fallen due for — the "backlog" the
 * backend materializes in one go the next time anyone reads the space.
 *
 * Mirrors the backend's own ceiling (`RecurrenceProjector.MAX_BACKLOG_OCCURRENCES` for
 * finance, `RecurrenceScheduler.MAX_BACKLOG_OCCURRENCES` for tasks — the same value on
 * both sides) so a form can say what is wrong with a start date before submitting, the
 * same way `leadTimeExceedsInterval` mirrors the lead-time rule. A fast client-side check,
 * not a replacement: the server remains authoritative and re-validates on submit.
 *
 * Lives in `shared` rather than being copied into both page slices because the calendar
 * arithmetic is identical and carries no domain meaning of its own — only the interval
 * names, which both modules spell the same way.
 */
export const MAX_PAST_OCCURRENCES = 365

export type RecurrenceIntervalName = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

const MS_PER_DAY = 86_400_000

/**
 * Builds a UTC date without `Date.UTC`'s two-digit-year rule, which silently maps year 50
 * to 1950 — a series anchored in year 0050 would otherwise be measured from the wrong
 * century. Months outside 0–11 are normalised here rather than left to `Date.UTC`, whose
 * own carry would be undone by the `setUTCFullYear` that follows.
 */
function utcDate(year: number, monthIndex: number, day: number): Date {
  const totalMonths = year * 12 + monthIndex
  const normalisedYear = Math.floor(totalMonths / 12)
  const normalisedMonth = totalMonths - normalisedYear * 12
  const date = new Date(Date.UTC(2000, normalisedMonth, day))
  date.setUTCFullYear(normalisedYear)
  return date
}

function parseIso(iso: string): Date | null {
  const match = /^(-?\d{4,6})-(\d{2})-(\d{2})$/.exec(iso)
  if (!match) return null
  const parsed = utcDate(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

/**
 * Date of the Nth occurrence, counting the anchor itself as occurrence 0. Monthly and
 * yearly anchoring on a day the target period lacks (the 31st, Feb 29th) clamps to that
 * period's last day, but every later occurrence is still derived from the original anchor
 * day — matching the backend, so a Jan-31 anchor lands on Feb 28 then back on Mar 31.
 */
function occurrenceDate(anchor: Date, intervalType: RecurrenceIntervalName, intervalCount: number, occurrenceNumber: number): Date {
  const steps = intervalCount * occurrenceNumber
  const anchorDay = anchor.getUTCDate()
  switch (intervalType) {
    case 'DAILY':
      return new Date(anchor.getTime() + steps * MS_PER_DAY)
    case 'WEEKLY':
      return new Date(anchor.getTime() + steps * 7 * MS_PER_DAY)
    case 'MONTHLY': {
      const monthIndex = anchor.getUTCMonth() + steps
      const lastDay = utcDate(anchor.getUTCFullYear(), monthIndex + 1, 0).getUTCDate()
      return utcDate(anchor.getUTCFullYear(), monthIndex, Math.min(anchorDay, lastDay))
    }
    case 'YEARLY': {
      const year = anchor.getUTCFullYear() + steps
      const lastDay = utcDate(year, anchor.getUTCMonth() + 1, 0).getUTCDate()
      return utcDate(year, anchor.getUTCMonth(), Math.min(anchorDay, lastDay))
    }
  }
}

/**
 * Number of occurrences falling on or before `horizon`, the anchor included.
 *
 * Located arithmetically rather than by stepping from occurrence 0: a user who mistypes
 * 1990 for 2026 would otherwise make the browser iterate thirteen thousand times on every
 * keystroke, and an anchor in year 1 would freeze the tab outright. The division only
 * estimates — month-end clamping means it knows calendar periods, not exact dates — so two
 * bounded correction steps settle it, exactly as the backend does.
 */
function occurrenceCountThrough(anchor: Date, intervalType: RecurrenceIntervalName, intervalCount: number, horizon: Date): number {
  if (horizon.getTime() < anchor.getTime()) return 0
  const days = (horizon.getTime() - anchor.getTime()) / MS_PER_DAY
  const months = (horizon.getUTCFullYear() - anchor.getUTCFullYear()) * 12 + (horizon.getUTCMonth() - anchor.getUTCMonth())
  const estimate = (() => {
    switch (intervalType) {
      case 'DAILY': return days / intervalCount
      case 'WEEKLY': return days / (7 * intervalCount)
      case 'MONTHLY': return months / intervalCount
      case 'YEARLY': return (horizon.getUTCFullYear() - anchor.getUTCFullYear()) / intervalCount
    }
  })()
  let n = Math.max(0, Math.floor(estimate))
  while (n > 0 && occurrenceDate(anchor, intervalType, intervalCount, n - 1).getTime() > horizon.getTime()) n--
  while (occurrenceDate(anchor, intervalType, intervalCount, n).getTime() <= horizon.getTime()) n++
  return n
}

/**
 * How many occurrences a series anchored at `anchorDate` has already fallen due for by
 * `today`, stopping at `endDate` when one is set. Returns 0 when the inputs are not usable
 * yet (empty or half-typed date, non-positive interval) — an incomplete form is the other
 * field validations' business, not this one's.
 */
export function pastOccurrenceCount(
  anchorDate: string, intervalType: RecurrenceIntervalName, intervalCount: number,
  endDate: string | null, today: string,
): number {
  if (intervalCount < 1) return 0
  const anchor = parseIso(anchorDate)
  const todayDate = parseIso(today)
  if (!anchor || !todayDate) return 0
  const end = endDate ? parseIso(endDate) : null
  const horizon = end && end.getTime() < todayDate.getTime() ? end : todayDate
  return occurrenceCountThrough(anchor, intervalType, intervalCount, horizon)
}
