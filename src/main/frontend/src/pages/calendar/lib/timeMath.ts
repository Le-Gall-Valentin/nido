/** Pixels per hour in every hour grid. The unit every position and every drop is computed in. */
export const HOUR_HEIGHT = 60
/** Every time a drag produces lands on a quarter hour. */
export const SNAP_MINUTES = 15
export const DAY_MINUTES = 1440

/** Accepts the API's "HH:mm:ss" as well as the form's "HH:mm". */
export function timeToMinutes(time: string): number {
  const [hours, minutes] = time.split(':')
  return Number(hours) * 60 + Number(minutes)
}

export function minutesToTime(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return `${String(hours).padStart(2, '0')}:${String(rest).padStart(2, '0')}`
}

export function snap(minutes: number): number {
  return Math.round(minutes / SNAP_MINUTES) * SNAP_MINUTES
}

/**
 * The time under the pointer in an hour column. `columnTop` must be the live top of the full-height
 * column, which moves with its scroller — so scrolling needs no separate correction here.
 */
export function minutesAt(pointerY: number, columnTop: number): number {
  const minutes = snap(((pointerY - columnTop) / HOUR_HEIGHT) * 60)
  return Math.min(DAY_MINUTES, Math.max(0, minutes))
}
