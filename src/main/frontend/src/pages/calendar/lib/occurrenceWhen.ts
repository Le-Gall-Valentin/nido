import type { CalendarOccurrence } from '@/entities/calendar'
import { formatPeriodLabel, formatSpan } from './periodLabel'

/** The same wording as the calendar's own headings: a day in full, a span shortened only as it can be. */
export function formatWhen(occurrence: CalendarOccurrence, allDayLabel: string, locale: string): string {
  const days = occurrence.startDate === occurrence.endDate
    ? formatPeriodLabel('day', occurrence.startDate, locale)
    : formatSpan(occurrence.startDate, occurrence.endDate, locale)
  if (occurrence.allDay) return `${days} · ${allDayLabel}`
  return `${days} · ${occurrence.startTime?.slice(0, 5)} – ${occurrence.endTime?.slice(0, 5)}`
}
