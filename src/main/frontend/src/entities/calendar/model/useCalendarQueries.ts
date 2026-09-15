import { useQuery } from '@tanstack/react-query'
import { useCalendarApi, useRecurringEventSeriesApi } from './calendarApiContext'

/** Every calendar query lives under this prefix, so one mutation can invalidate all windows. */
export function calendarKey(spaceId: string) {
  return ['calendar', spaceId] as const
}

export function occurrencesKey(spaceId: string, from: string, to: string) {
  return ['calendar', spaceId, 'occurrences', from, to] as const
}

export function useOccurrences(spaceId: string | undefined, from: string, to: string) {
  const api = useCalendarApi()
  return useQuery({
    queryKey: occurrencesKey(spaceId ?? '', from, to),
    queryFn: () => api.listOccurrences(spaceId as string, from, to),
    enabled: !!spaceId,
    // Paging back and forth through months would otherwise blank the grid on every step. Keeping
    // the previous window on screen while the next one loads is what makes navigation feel solid.
    placeholderData: (previous) => previous,
  })
}

export function recurringEventSeriesKey(spaceId: string) {
  return ['calendar', spaceId, 'recurring-series'] as const
}

export function useRecurringEventSeries(spaceId: string | undefined) {
  const api = useRecurringEventSeriesApi()
  return useQuery({
    queryKey: recurringEventSeriesKey(spaceId ?? ''),
    queryFn: () => api.listRecurringEventSeries(spaceId as string),
    enabled: !!spaceId,
  })
}
