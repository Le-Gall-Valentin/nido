import type { CalendarOccurrence } from '@/entities/calendar'

/**
 * Whether an occurrence can be moved to another day or time — the twin of what
 * `useRescheduleOccurrence` knows how to write, which is why the two live together.
 *
 * It must have something to rewrite: a row, or — for an event series — a slot, which a drop
 * detaches through the same upsert as "edit this occurrence". A projected recurring task has
 * neither a row nor a slot mechanism, so it stays put. And its date must be something the calendar
 * has any business changing: a finance transaction's date is an accounting fact, and a savings
 * deadline belongs to its goal, not to whoever is looking at the month. Hence the explicit source
 * list rather than relying on `materialized` alone.
 */
export function canReschedule(occurrence: CalendarOccurrence): boolean {
  if (occurrence.source === 'EVENT') {
    // A projected occurrence of a series has no row, but it has a slot: a drop detaches just that
    // one through the same upsert as "edit this occurrence".
    return occurrence.materialized || (occurrence.seriesId !== null && occurrence.originalDate !== null)
  }
  return occurrence.materialized && (occurrence.source === 'TASK' || occurrence.source === 'MEAL')
}
