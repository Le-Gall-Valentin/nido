/**
 * Calendar dates as the dashboard prints them. An ISO date is a day on the household's calendar, not
 * an instant: every format reads it at UTC midnight and formats it in UTC, so no machine zone can move
 * it to the day before.
 */
function atUtcMidnight(iso: string): Date {
  return new Date(`${iso}T00:00:00Z`)
}

function format(iso: string, locale: string, options: Intl.DateTimeFormatOptions): string {
  return new Intl.DateTimeFormat(locale, { ...options, timeZone: 'UTC' }).format(atUtcMidnight(iso))
}

function capitalise(text: string, locale: string): string {
  return text.charAt(0).toLocaleUpperCase(locale) + text.slice(1)
}

/** "Samedi 26 septembre" — the page title. */
export function formatLongDay(iso: string, locale: string): string {
  return capitalise(format(iso, locale, { weekday: 'long', day: 'numeric', month: 'long' }), locale)
}

/** "lun. 28" */
export function formatShortDay(iso: string, locale: string): string {
  return format(iso, locale, { weekday: 'short', day: 'numeric' })
}

/** "22 sept." */
export function formatDayMonth(iso: string, locale: string): string {
  return format(iso, locale, { day: 'numeric', month: 'short' })
}

/** "jeudi" */
export function formatWeekday(iso: string, locale: string): string {
  return format(iso, locale, { weekday: 'long' })
}

/** "juin 2027" */
export function formatMonthYear(iso: string, locale: string): string {
  return format(iso, locale, { month: 'long', year: 'numeric' })
}

/** "Septembre", from "2026-09". */
export function formatMonthName(yearMonth: string, locale: string): string {
  return capitalise(format(`${yearMonth}-01`, locale, { month: 'long' }), locale)
}

export function addDaysIso(iso: string, days: number): string {
  const date = atUtcMidnight(iso)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

/** The calendar sends `HH:mm` or `HH:mm:ss`; the dashboard shows `HH:mm`. */
export function shortTime(time: string): string {
  return time.slice(0, 5)
}
