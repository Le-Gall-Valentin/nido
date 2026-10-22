import { useState } from 'react'
import type { DayRule, RecurrenceInterval, Weekday } from '@/entities/calendar'
import { draftOf, followStart, toggleDay, type RecurrenceDraft } from '../lib/recurrenceDraft'
import type { Recurrence } from '../lib/seriesInput'

/** The event form's recurrence box: how the event repeats, as it is being filled in (see RecurrenceDraft). */
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
    /** To call with every value the start date field takes, a cleared one included. */
    followStart: (startDate: string) => setDraft((current) => followStart(current, startDate)),
  }
}

export type RecurrenceDraftState = ReturnType<typeof useRecurrenceDraft>
