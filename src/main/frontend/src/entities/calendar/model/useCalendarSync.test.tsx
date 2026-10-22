import { describe, it, expect, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider, QueryObserver } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useInvalidateOnAnyWrite } from '@/shared/lib'
import { CalendarApiProvider } from './CalendarApiProvider'
import type { CalendarApi } from './calendarApiContext'
import { CalendarSyncError } from '../api/calendarSyncErrors'
import { calendarKey } from './useCalendarQueries'
import {
  useAnalyseImport, useCreateSubscriptionLink, useExportCalendar, useFollowExternalFeed, useImportFile, useRefreshExternalFeed,
  useRemoveExternalFeed, useRevokeSubscriptionLink, useUpdateExternalFeed,
} from './useCalendarSync'

type Mutation = () => { mutateAsync: (a: unknown) => Promise<unknown>; isSuccess: boolean }

function setup(failing: Partial<Record<keyof CalendarApi, CalendarSyncError>> = {}) {
  const api = {
    createSubscriptionLink: vi.fn().mockResolvedValue({}), revokeSubscriptionLink: vi.fn().mockResolvedValue(undefined),
    followExternalFeed: vi.fn().mockResolvedValue({}), refreshExternalFeed: vi.fn().mockResolvedValue({}),
    updateExternalFeed: vi.fn().mockResolvedValue({}),
    removeExternalFeed: vi.fn().mockResolvedValue(undefined), importFile: vi.fn().mockResolvedValue({}),
    analyseImport: vi.fn().mockResolvedValue({}), exportCalendar: vi.fn().mockResolvedValue({ blob: new Blob(), filename: 'x.ics' }),
    ...Object.fromEntries(Object.entries(failing).map(([method, error]) => [method, vi.fn().mockRejectedValue(error)])),
  } as unknown as CalendarApi
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const occurrences = vi.fn().mockResolvedValue([])
  const links = vi.fn().mockResolvedValue([])
  // Observed, as a page showing them would: only active queries are read again.
  new QueryObserver(client, { queryKey: ['calendar', 's1', 'occurrences', 'a', 'b'], queryFn: occurrences }).subscribe(() => {})
  new QueryObserver(client, { queryKey: ['calendar', 's1', 'subscription-links'], queryFn: links }).subscribe(() => {})
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}><CalendarApiProvider api={api}>{children}</CalendarApiProvider></QueryClientProvider>
  )
  return { wrapper, occurrences, links }
}

/** Listens as the calendar page does, reading the calendar again after any write that does not say otherwise. */
function listenAsTheCalendarPage(wrapper: ({ children }: { children: ReactNode }) => ReactNode) {
  renderHook(() => useInvalidateOnAnyWrite(calendarKey('s1')), { wrapper })
}

describe('calendar sync hooks', () => {
  it.each([
    ['follow', () => useFollowExternalFeed('s1'), { url: 'u', name: null, color: null }],
    ['refresh', () => useRefreshExternalFeed('s1'), 'f1'],
    ['remove', () => useRemoveExternalFeed('s1'), 'f1'],
    ['import', () => useImportFile('s1'), new Blob()],
  ] as const)('reads the occurrences again after %s', async (_name, hook, arg) => {
    const { wrapper, occurrences } = setup()
    await waitFor(() => expect(occurrences).toHaveBeenCalledTimes(1))
    const { result } = renderHook(hook as Mutation, { wrapper })

    await result.current.mutateAsync(arg)

    await waitFor(() => expect(occurrences).toHaveBeenCalledTimes(2))
  })

  it.each([
    ['create', () => useCreateSubscriptionLink('s1'), { label: 'x', sources: [], onlyMine: false }],
    ['revoke', () => useRevokeSubscriptionLink('s1'), 'l1'],
  ] as const)('reads only the addresses again after %s, on the calendar page too', async (_name, hook, arg) => {
    const { wrapper, occurrences, links } = setup()
    listenAsTheCalendarPage(wrapper)
    await waitFor(() => expect(links).toHaveBeenCalledTimes(1))
    const { result } = renderHook(hook as Mutation, { wrapper })

    await result.current.mutateAsync(arg)

    // Told in the same round as every listener to the cache: the page's own has had its say by then.
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(links).toHaveBeenCalledTimes(2)
    expect(occurrences).toHaveBeenCalledTimes(1)
  })

  it.each([
    ['export', () => useExportCalendar('s1'), { sources: ['EVENT'], onlyMine: false }],
    ['analysis', () => useAnalyseImport('s1'), new Blob()],
  ] as const)('reads nothing again after an %s, which writes nothing, on the calendar page too', async (_name, hook, arg) => {
    const { wrapper, occurrences, links } = setup()
    listenAsTheCalendarPage(wrapper)
    await waitFor(() => expect(links).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(occurrences).toHaveBeenCalledTimes(1))
    const { result } = renderHook(hook as Mutation, { wrapper })

    await result.current.mutateAsync(arg)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(links).toHaveBeenCalledTimes(1)
    expect(occurrences).toHaveBeenCalledTimes(1)
  })

  it('reads the addresses again when the one revoked was already gone', async () => {
    const { wrapper, occurrences, links } = setup({ revokeSubscriptionLink: new CalendarSyncError('subscription_link_not_found') })
    await waitFor(() => expect(links).toHaveBeenCalledTimes(1))
    const { result } = renderHook(() => useRevokeSubscriptionLink('s1'), { wrapper })

    await expect(result.current.mutateAsync('l1')).rejects.toMatchObject({ reason: 'subscription_link_not_found' })

    await waitFor(() => expect(links).toHaveBeenCalledTimes(2))
    expect(occurrences).toHaveBeenCalledTimes(1)
  })

  it.each([
    ['update', () => useUpdateExternalFeed('s1'), 'updateExternalFeed', { feedId: 'f1', name: 'École', color: null }],
    ['remove', () => useRemoveExternalFeed('s1'), 'removeExternalFeed', 'f1'],
    ['refresh', () => useRefreshExternalFeed('s1'), 'refreshExternalFeed', 'f1'],
  ] as const)('reads the calendar again when the calendar to %s was already gone', async (_name, hook, method, arg) => {
    const { wrapper, occurrences } = setup({ [method]: new CalendarSyncError('external_feed_not_found') })
    await waitFor(() => expect(occurrences).toHaveBeenCalledTimes(1))
    const { result } = renderHook(hook as Mutation, { wrapper })

    await expect(result.current.mutateAsync(arg)).rejects.toMatchObject({ reason: 'external_feed_not_found' })

    await waitFor(() => expect(occurrences).toHaveBeenCalledTimes(2))
  })

  it('reads nothing again after a failure for any other reason', async () => {
    const { wrapper, occurrences, links } = setup({
      revokeSubscriptionLink: new CalendarSyncError('rate_limited'), removeExternalFeed: new CalendarSyncError('server'),
    })
    await waitFor(() => expect(links).toHaveBeenCalledTimes(1))
    const revoke = renderHook(() => useRevokeSubscriptionLink('s1'), { wrapper })
    const remove = renderHook(() => useRemoveExternalFeed('s1'), { wrapper })

    await expect(revoke.result.current.mutateAsync('l1')).rejects.toBeInstanceOf(CalendarSyncError)
    await expect(remove.result.current.mutateAsync('f1')).rejects.toBeInstanceOf(CalendarSyncError)

    expect(links).toHaveBeenCalledTimes(1)
    expect(occurrences).toHaveBeenCalledTimes(1)
  })
})
