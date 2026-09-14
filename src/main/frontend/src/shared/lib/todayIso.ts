/**
 * Today's date as `YYYY-MM-DD`, on a given calendar.
 *
 * A household agrees on one calendar: the rent falls due on the household's date, not on the date
 * of whichever member is travelling. So screens that show a space pass that space's zone, and get
 * the same answer the backend computes for it — the two have to agree, because the backend writes
 * rows based on what is due "today" while the frontend decides what to show as late.
 *
 * Deliberately not `new Date().toISOString().slice(0, 10)`, which converts to UTC first: west of
 * Greenwich that returns tomorrow's date for the last hours of every evening, east of it
 * yesterday's for the first hours of every morning.
 *
 * Built with `en-CA` because it formats as `YYYY-MM-DD` — the shape the API speaks — so there is no
 * re-assembling of parts and no chance of a month and a day swapping places.
 *
 * `now` is injectable so callers' tests can pin a date instead of depending on when the suite runs.
 * Omitting `zone` reads the viewer's own calendar, which is what a screen not tied to a space wants.
 */
export function todayIso(now: Date = new Date(), zone?: string): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: zone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now)
}

/** The same, as `YYYY-MM` — what the finance page opens on. */
export function monthIso(now: Date = new Date(), zone?: string): string {
  return todayIso(now, zone).slice(0, 7)
}
