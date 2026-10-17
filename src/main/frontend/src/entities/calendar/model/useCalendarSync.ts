import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCalendarSyncApi } from './calendarApiContext'
import { calendarKey } from './useCalendarQueries'
import type { ExportableSource, ExternalFeedInput, SubscriptionLinkInput } from './types'

export function subscriptionLinksKey(spaceId: string) {
  return ['calendar', spaceId, 'subscription-links'] as const
}

export function externalFeedsKey(spaceId: string) {
  return ['calendar', spaceId, 'external-feeds'] as const
}

/** Every write here changes what the calendar shows or lists: the whole calendar prefix is read again. */
function useCalendarInvalidation(spaceId: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: calendarKey(spaceId) })
}

/** An address changes nothing the calendar shows: only the list of addresses is read again. */
function useLinksInvalidation(spaceId: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: subscriptionLinksKey(spaceId) })
}

export function useSubscriptionLinks(spaceId: string | undefined) {
  const api = useCalendarSyncApi()
  return useQuery({
    queryKey: subscriptionLinksKey(spaceId ?? ''),
    queryFn: () => api.listSubscriptionLinks(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useCreateSubscriptionLink(spaceId: string) {
  const api = useCalendarSyncApi()
  const invalidate = useLinksInvalidation(spaceId)
  return useMutation({ mutationFn: (input: SubscriptionLinkInput) => api.createSubscriptionLink(spaceId, input), onSuccess: invalidate })
}

export function useRevokeSubscriptionLink(spaceId: string) {
  const api = useCalendarSyncApi()
  const invalidate = useLinksInvalidation(spaceId)
  return useMutation({ mutationFn: (linkId: string) => api.revokeSubscriptionLink(spaceId, linkId), onSuccess: invalidate })
}

export function useExternalFeeds(spaceId: string | undefined) {
  const api = useCalendarSyncApi()
  return useQuery({
    queryKey: externalFeedsKey(spaceId ?? ''),
    queryFn: () => api.listExternalFeeds(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useFollowExternalFeed(spaceId: string) {
  const api = useCalendarSyncApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({ mutationFn: (input: ExternalFeedInput) => api.followExternalFeed(spaceId, input), onSuccess: invalidate })
}

export function useUpdateExternalFeed(spaceId: string) {
  const api = useCalendarSyncApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({
    mutationFn: ({ feedId, name, color }: { feedId: string; name: string; color: string | null }) =>
      api.updateExternalFeed(spaceId, feedId, { name, color }),
    onSuccess: invalidate,
  })
}

export function useRemoveExternalFeed(spaceId: string) {
  const api = useCalendarSyncApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({ mutationFn: (feedId: string) => api.removeExternalFeed(spaceId, feedId), onSuccess: invalidate })
}

export function useRefreshExternalFeed(spaceId: string) {
  const api = useCalendarSyncApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({ mutationFn: (feedId: string) => api.refreshExternalFeed(spaceId, feedId), onSuccess: invalidate })
}

/** Reads a file as an import would, writing nothing: nothing to refresh. */
export function useAnalyseImport(spaceId: string) {
  const api = useCalendarSyncApi()
  return useMutation({ mutationFn: (file: Blob) => api.analyseImport(spaceId, file) })
}

export function useImportFile(spaceId: string) {
  const api = useCalendarSyncApi()
  const invalidate = useCalendarInvalidation(spaceId)
  return useMutation({ mutationFn: (file: Blob) => api.importFile(spaceId, file), onSuccess: invalidate })
}

export function useExportCalendar(spaceId: string) {
  const api = useCalendarSyncApi()
  return useMutation({
    mutationFn: ({ sources, onlyMine }: { sources: ExportableSource[]; onlyMine: boolean }) =>
      api.exportCalendar(spaceId, sources, onlyMine),
  })
}
