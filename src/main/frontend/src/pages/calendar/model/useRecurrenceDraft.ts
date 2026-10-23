import { useState } from 'react'
import type { DayRule, RecurrenceInterval, Weekday } from '@/entities/calendar'
import { draftOf, toggleDay, type RecurrenceDraft } from '../lib/recurrenceDraft'
import type { Recurrence } from '../lib/seriesInput'

/**
 * The event form's recurrence box: how the event repeats, as it is being filled in (see RecurrenceDraft). Moving the
 * start changes nothing here: what depends on it is read off it.
 */
export function useRecurrenceDraft(initial: Recurrence | null, initialStart: string) {
  const [draft, setDraft] = useState<RecurrenceDraft>(() => draftOf(initial, initialStart))

  const edit = (changes: Partial<RecurrenceDraft>) => setDraft((current) => ({ ...current, ...changes }))

  return {
    draft,
    setIntervalCount: (intervalCount: number) => edit({ intervalCount }),
    setIntervalType: (intervalType: RecurrenceInterval) => edit({ intervalType }),
    setDayRule: (dayRule: DayRule) => edit({ dayRule }),
    setUntil: (until: string | null) => edit({ until }),
    toggleDay: (day: Weekday) => setDraft((current) => toggleDay(current, day)),
  }
}

export type RecurrenceDraftState = ReturnType<typeof useRecurrenceDraft>
