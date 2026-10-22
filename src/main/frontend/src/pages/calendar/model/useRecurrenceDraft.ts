import { useState } from 'react'
import type { DayRule, RecurrenceInterval, Weekday } from '@/entities/calendar'
import { draftOf, followStart, toggleDay, type RecurrenceDraft } from '../lib/recurrenceDraft'
import type { Recurrence } from '../lib/seriesInput'

/**
 * The event form's recurrence box: how the event repeats, as it is being filled in (see RecurrenceDraft), and whether
 * the last move of the start made the day rule fall back — which a reader who cannot see the list change is told.
 */
export function useRecurrenceDraft(initial: Recurrence | null, initialStart: string) {
  const [draft, setDraft] = useState<RecurrenceDraft>(() => draftOf(initial, initialStart))
  const [ruleFellBack, setRuleFellBack] = useState(false)

  const edit = (changes: Partial<RecurrenceDraft>) => {
    setRuleFellBack(false)
    setDraft((current) => ({ ...current, ...changes }))
  }

  return {
    draft,
    ruleFellBack,
    setIntervalCount: (intervalCount: number) => edit({ intervalCount }),
    setIntervalType: (intervalType: RecurrenceInterval) => edit({ intervalType }),
    setDayRule: (dayRule: DayRule) => edit({ dayRule }),
    setUntil: (until: string | null) => edit({ until }),
    toggleDay: (day: Weekday) => {
      setRuleFellBack(false)
      setDraft((current) => toggleDay(current, day))
    },
    /** To call with every value the start date field takes, a cleared one included. */
    followStart: (startDate: string) => {
      const next = followStart(draft, startDate)
      setRuleFellBack(next.dayRule !== draft.dayRule)
      setDraft(next)
    },
  }
}

export type RecurrenceDraftState = ReturnType<typeof useRecurrenceDraft>
