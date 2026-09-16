import type { CalendarOccurrence } from '@/entities/calendar'

/**
 * Whether an occurrence can be dragged to another day.
 *
 * Two conditions, and both matter. It must have a row to rewrite — a projected occurrence is a
 * date computed from a template, so there is nothing to move. And its date must be something the
 * calendar has any business changing: a finance transaction's date is an accounting fact, and a
 * savings deadline belongs to the goal, not to whoever is looking at the month. Hence the explicit
 * source list rather than relying on `materialized` alone.
 */
export function isDraggable(occurrence: CalendarOccurrence): boolean {
  if (!occurrence.materialized) return false
  return occurrence.source === 'EVENT' || occurrence.source === 'TASK' || occurrence.source === 'MEAL'
}
