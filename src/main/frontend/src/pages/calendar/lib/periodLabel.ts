import { addDays, startOfWeek, type CalendarView } from './calendarWindow'

/** Parsed as UTC, like every other date computation here, so no zone can shift the day. */
function parse(iso: string): Date {
  return new Date(`${iso}T00:00:00Z`)
}

function format(iso: string, locale: string, options: Intl.DateTimeFormatOptions): string {
  return new Intl.DateTimeFormat(locale, { ...options, timeZone: 'UTC' }).format(parse(iso))
}

/**
 * Capitalises the first letter.
 *
 * French writes month and weekday names in lower case mid-sentence, and Intl returns them that
 * way — correct inside a phrase, but this string stands alone as the heading of the view, where
 * a lower-case opening reads like a fragment.
 */
function asHeading(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1)
}

/**
 * Says which period is on screen.
 *
 * Each view names itself at the granularity it shows: a month gives its name and year, a week
 * gives the span it covers, a day gives itself in full. Without it, paging through the calendar
 * moves the grid with nothing to say where you have arrived.
 *
 * The week's span is spelled out only as far as it has to be — the month is written once when
 * both ends share it, and the year once when both ends share that — so the common case stays
 * short and only a week straddling a boundary pays for the extra words. See formatSpan.
 */
export function formatPeriodLabel(view: CalendarView, iso: string, locale: string): string {
  if (view === 'month') {
    return asHeading(format(iso, locale, { month: 'long', year: 'numeric' }))
  }

  if (view === 'day') {
    return asHeading(format(iso, locale, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }))
  }

  const from = startOfWeek(iso)
  return formatSpan(from, addDays(from, 6), locale)
}

/**
 * A span of days, spelled out only as far as it has to be — shared by the week heading and by any
 * occurrence that runs over several days, so both read the same way.
 */
export function formatSpan(from: string, to: string, locale: string): string {
  const sameMonth = from.slice(0, 7) === to.slice(0, 7)
  const sameYear = from.slice(0, 4) === to.slice(0, 4)

  if (sameMonth) {
    const start = format(from, locale, { day: 'numeric' })
    return `${start} – ${asHeading(format(to, locale, { day: 'numeric', month: 'long', year: 'numeric' }))}`
  }
  if (sameYear) {
    const start = format(from, locale, { day: 'numeric', month: 'short' })
    return `${start} – ${format(to, locale, { day: 'numeric', month: 'short', year: 'numeric' })}`
  }
  const start = format(from, locale, { day: 'numeric', month: 'short', year: 'numeric' })
  return `${start} – ${format(to, locale, { day: 'numeric', month: 'short', year: 'numeric' })}`
}
