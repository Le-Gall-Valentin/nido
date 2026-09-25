import { useCallback, useState } from 'react'
import type { CalendarOccurrence, ScheduleChange } from '@/entities/calendar'

/** What an action on one occurrence of a series applies to — itself, or the series it comes from. */
export type OccurrenceScope = 'occurrence' | 'series'

export type ScopedAction = 'edit' | 'delete'

/**
 * The one dialog the calendar has open, if any. One value rather than a flag per dialog: every step
 * from one to the next closes the first, and two open at once is a state no gesture should reach.
 */
export type CalendarDialog =
  | { kind: 'day'; date: string }
  // The id, not the object: an occurrence held here is a snapshot, and every write that refetches
  // the window would leave the open dialog showing what was true before it — joining an event and
  // watching "Nobody" stay on screen is how that shows up.
  | { kind: 'occurrence'; sourceId: string }
  // An action landed on an occurrence of a series, and whether it means the series is still open.
  | { kind: 'scope'; occurrence: CalendarOccurrence; action: ScopedAction }
  // `returnToDay` when opened from a day's detail, so the event is seen landing in that day; and
  // `schedule` when a time was picked out in the grid, which the form then opens on exactly.
  | { kind: 'create'; date: string; returnToDay: boolean; schedule?: ScheduleChange }
  | { kind: 'edit'; occurrence: CalendarOccurrence; detachSlot: { seriesId: string; date: string } | null }
  | { kind: 'delete'; occurrence: CalendarOccurrence }
  // "The whole series" was the answer: the series is edited in the event form, or deleted.
  | { kind: 'series'; seriesId: string; action: ScopedAction }
  | { kind: 'transfer'; occurrence: CalendarOccurrence; operation: 'copy' | 'move' }
  | { kind: 'meal'; date: string }
  | { kind: 'manage-series' }

/** Which dialog the calendar shows, and the steps that lead from one to the next. */
export function useCalendarDialog() {
  const [open, setOpen] = useState<CalendarDialog | null>(null)
  const close = useCallback(() => setOpen(null), [])

  /** Editing or deleting: an occurrence of a series must first be asked about. */
  const act = useCallback((occurrence: CalendarOccurrence, action: ScopedAction) => {
    if (occurrence.seriesId && occurrence.source === 'EVENT') setOpen({ kind: 'scope', occurrence, action })
    else if (action === 'edit') setOpen({ kind: 'edit', occurrence, detachSlot: null })
    else setOpen({ kind: 'delete', occurrence })
  }, [])

  const answerScope = useCallback((scope: OccurrenceScope) => setOpen((current) => {
    if (current?.kind !== 'scope') return current
    const { occurrence, action } = current
    if (scope === 'series') {
      return occurrence.seriesId ? { kind: 'series', seriesId: occurrence.seriesId, action } : null
    }
    if (action === 'delete') return { kind: 'delete', occurrence }
    const detachSlot = occurrence.seriesId && occurrence.originalDate
      ? { seriesId: occurrence.seriesId, date: occurrence.originalDate }
      : null
    return { kind: 'edit', occurrence, detachSlot }
  }), [])

  /** Closing the event form, which hands back to the day it was opened from. */
  const closeForm = useCallback(() => setOpen((current) =>
    current?.kind === 'create' && current.returnToDay ? { kind: 'day', date: current.date } : null), [])

  return { open, show: setOpen, close, act, answerScope, closeForm }
}

export type CalendarDialogState = ReturnType<typeof useCalendarDialog>
