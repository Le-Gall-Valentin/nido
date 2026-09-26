import { addDaysIso, formatWeekday } from './dates'

/** Ascending ISO dates grouped into runs of consecutive days. */
export function groupConsecutiveDays(days: string[]): { from: string; to: string }[] {
  const runs: { from: string; to: string }[] = []
  for (const day of days) {
    const last = runs[runs.length - 1]
    if (last && addDaysIso(last.to, 1) === day) {
      last.to = day
    } else {
      runs.push({ from: day, to: day })
    }
  }
  return runs
}

/**
 * "jeudi", "de jeudi à dimanche", "mardi, de jeudi à vendredi" — the days with nothing on the menu.
 * The wording of a run comes from the caller's translations, so this stays free of any language.
 */
export function formatDayRanges(days: string[], locale: string, range: (from: string, to: string) => string): string {
  return groupConsecutiveDays(days)
    .map(({ from, to }) => (from === to
      ? formatWeekday(from, locale)
      : range(formatWeekday(from, locale), formatWeekday(to, locale))))
    .join(', ')
}
