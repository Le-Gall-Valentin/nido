import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useCalendarApi, useRecurringEventSeriesApi } from './calendarApiContext'
import { calendarKey } from './useCalendarQueries'
import type { EventInput, RecurringEventSeriesInput } from './types'

/**
 * Every calendar mutation invalidates the whole `['calendar', spaceId]` prefix.
 *
 * A single write can affect several cached windows at once — a multi-day event spans weeks, a
 * series edit re-dates every month, an exclusion changes one slot in whichever window shows it.
 * Narrowing the invalidation would leave a stale month one swipe away.
 */
function useCalendarInvalidation(spaceId: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: calendarKey(spaceId) })
}

export function useCreateEvent(spaceId: string) {
  const api = useCalendarApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: (input: EventInput) => api.createEvent(spaceId, input),
    onSuccess: invalidate,
  })
}

export function useUpdateEvent(spaceId: string) {
  const api = useCalendarApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ eventId, input }: { eventId: string; input: EventInput }) =>
      api.updateEvent(spaceId, eventId, input),
    onSuccess: invalidate,
  })
}

export function useDeleteEvent(spaceId: string) {
  const api = useCalendarApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: (eventId: string) => api.deleteEvent(spaceId, eventId),
    onSuccess: invalidate,
  })
}

export function useJoinEvent(spaceId: string) {
  const api = useCalendarApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: (eventId: string) => api.joinEvent(spaceId, eventId),
    onSuccess: invalidate,
  })
}

export function useLeaveEvent(spaceId: string) {
  const api = useCalendarApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: (eventId: string) => api.leaveEvent(spaceId, eventId),
    onSuccess: invalidate,
  })
}

/** After a transfer: the destination's calendar changed too, and it is cheap to invalidate even if never fetched. */
function useTransferInvalidation(spaceId: string) {
  const queryClient = useQueryClient()
  return (_result: unknown, { destinationSpaceId }: { destinationSpaceId: string }) => Promise.all([
    queryClient.invalidateQueries({ queryKey: calendarKey(spaceId) }),
    queryClient.invalidateQueries({ queryKey: calendarKey(destinationSpaceId) }),
  ])
}

export function useCopyEvent(spaceId: string) {
  const api = useCalendarApi()
  const invalidate = useTransferInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ eventId, destinationSpaceId }: { eventId: string; destinationSpaceId: string }) =>
      api.copyEvent(spaceId, eventId, destinationSpaceId),
    onSuccess: invalidate,
  })
}

export function useMoveEvent(spaceId: string) {
  const api = useCalendarApi()
  const invalidate = useTransferInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ eventId, destinationSpaceId }: { eventId: string; destinationSpaceId: string }) =>
      api.moveEvent(spaceId, eventId, destinationSpaceId),
    onSuccess: invalidate,
  })
}

interface OccurrenceTransfer { seriesId: string; date: string; destinationSpaceId: string }

export function useCopyOccurrence(spaceId: string) {
  const api = useRecurringEventSeriesApi()
  const invalidate = useTransferInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ seriesId, date, destinationSpaceId }: OccurrenceTransfer) =>
      api.copyOccurrence(spaceId, seriesId, date, destinationSpaceId),
    onSuccess: invalidate,
  })
}

export function useMoveOccurrence(spaceId: string) {
  const api = useRecurringEventSeriesApi()
  const invalidate = useTransferInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ seriesId, date, destinationSpaceId }: OccurrenceTransfer) =>
      api.moveOccurrence(spaceId, seriesId, date, destinationSpaceId),
    onSuccess: invalidate,
  })
}

export function useCreateRecurringEventSeries(spaceId: string) {
  const api = useRecurringEventSeriesApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: (input: RecurringEventSeriesInput) => api.createRecurringEventSeries(spaceId, input),
    onSuccess: invalidate,
  })
}

export function useUpdateRecurringEventSeries(spaceId: string) {
  const api = useRecurringEventSeriesApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ seriesId, input }: { seriesId: string; input: RecurringEventSeriesInput }) =>
      api.updateRecurringEventSeries(spaceId, seriesId, input),
    onSuccess: invalidate,
  })
}

export function useDeleteRecurringEventSeries(spaceId: string) {
  const api = useRecurringEventSeriesApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: (seriesId: string) => api.deleteRecurringEventSeries(spaceId, seriesId),
    onSuccess: invalidate,
  })
}

export function useDetachOccurrence(spaceId: string) {
  const api = useRecurringEventSeriesApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ seriesId, date, input }: { seriesId: string; date: string; input: EventInput }) =>
      api.detachOccurrence(spaceId, seriesId, date, input),
    onSuccess: invalidate,
  })
}

export function useExcludeOccurrence(spaceId: string) {
  const api = useRecurringEventSeriesApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ seriesId, date }: { seriesId: string; date: string }) =>
      api.excludeOccurrence(spaceId, seriesId, date),
    onSuccess: invalidate,
  })
}
