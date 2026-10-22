import type { DayRule, RecurrenceInterval, Weekday } from '@/entities/calendar'
import { WEEKDAYS, possibleDayRules, weekdayOf } from './recurrenceRule'
import type { Recurrence } from './seriesInput'

/**
 * How an event repeats, as the form holds it while it is filled in. A weekly series always repeats on its start's day,
 * so that day is never stored: it is read off the start each time, and `extraDays` holds only the days chosen beyond
 * it. Moving the start then never edits the days chosen: a one-day series follows its start — through a cleared date
 * and through the dates a typed one passes on its way too —, a day the start left was never chosen and is not kept,
 * and a day that was chosen is never dropped, whatever the order of the clicks.
 */
export interface RecurrenceDraft {
  intervalType: RecurrenceInterval
  intervalCount: number
  until: string | null
  /** The days a weekly series repeats on besides its start's. */
  extraDays: Weekday[]
  dayRule: DayRule
}

/** The draft of how a series repeats, its days beyond the start shown; a new one is weekly, on its start's day, with no end. */
export function draftOf(recurrence: Recurrence | null, startDate: string): RecurrenceDraft {
  if (!recurrence) return { intervalType: 'WEEKLY', intervalCount: 1, until: null, extraDays: [], dayRule: 'SAME_DAY' }
  const { weekdays, ...rest } = recurrence
  const startDay = weekdayOf(startDate)
  return { ...rest, extraDays: weekdays.filter((day) => day !== startDay) }
}

/** The days a weekly series is shown and sent with, Monday first: its start's, and those chosen beyond it. */
export function shownDays(draft: RecurrenceDraft, startDate: string): Weekday[] {
  const startDay = weekdayOf(startDate)
  return WEEKDAYS.filter((day) => day === startDay || draft.extraDays.includes(day))
}

export function toggleDay(draft: RecurrenceDraft, day: Weekday): RecurrenceDraft {
  return {
    ...draft,
    extraDays: draft.extraDays.includes(day) ? draft.extraDays.filter((chosen) => chosen !== day) : [...draft.extraDays, day],
  }
}

/**
 * The draft once its start moved: a day rule the new start cannot give falls back to the same day. A field holding no
 * date changes nothing — the date is on its way.
 */
export function followStart(draft: RecurrenceDraft, startDate: string): RecurrenceDraft {
  if (!weekdayOf(startDate) || possibleDayRules(startDate).includes(draft.dayRule)) return draft
  return { ...draft, dayRule: 'SAME_DAY' }
}

/** What is sent: days for a weekly series only — its start's always among them —, a day rule for a monthly or yearly one only. */
export function toRecurrence(draft: RecurrenceDraft, startDate: string): Recurrence {
  return {
    intervalType: draft.intervalType,
    intervalCount: draft.intervalCount,
    until: draft.until,
    weekdays: draft.intervalType === 'WEEKLY' ? shownDays(draft, startDate) : [],
    dayRule: draft.intervalType === 'MONTHLY' || draft.intervalType === 'YEARLY' ? draft.dayRule : 'SAME_DAY',
  }
}
