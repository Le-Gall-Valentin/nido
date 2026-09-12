/**
 * Today's date in the viewer's own timezone, as `YYYY-MM-DD`.
 *
 * Deliberately not `new Date().toISOString().slice(0, 10)`, which converts to UTC first:
 * west of Greenwich that returns tomorrow's date for the last hours of every evening,
 * east of it yesterday's for the first hours of every morning. A date the user reads as
 * "today" on their own calendar is a civil date, not an instant, so it is built from the
 * local calendar fields rather than from an ISO instant.
 *
 * `now` is injectable so callers' tests can pin a date instead of depending on when the
 * suite happens to run.
 */
export function todayIso(now: Date = new Date()): string {
  const year = String(now.getFullYear()).padStart(4, '0')
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
