import { describe, it, expect, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider, QueryObserver } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { CalendarApiProvider } from './CalendarApiProvider'
import type { CalendarApi } from './calendarApiContext'
import {
  useCreateSubscriptionLink, useFollowExternalFeed, useImportFile, useRefreshExternalFeed,
  useRemoveExternalFeed, useRevokeSubscriptionLink,
} from './useCalendarSync'

function setup() {
  const api = {
    createSubscriptionLink: vi.fn().mockResolvedValue({}), revokeSubscriptionLink: vi.fn().mockResolvedValue(undefined),
    followExternalFeed: vi.fn().mockResolvedValue({}), refreshExternalFeed: vi.fn().mockResolvedValue({}),
    removeExternalFeed: vi.fn().mockResolvedValue(undefined), importFile: vi.fn().mockResolvedValue({}),
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

describe('calendar sync hooks', () => {
  it.each([
    ['follow', () => useFollowExternalFeed('s1'), { url: 'u', name: null, color: null }],
    ['refresh', () => useRefreshExternalFeed('s1'), 'f1'],
    ['remove', () => useRemoveExternalFeed('s1'), 'f1'],
    ['import', () => useImportFile('s1'), new Blob()],
  ] as const)('reads the occurrences again after %s', async (_name, hook, arg) => {
    const { wrapper, occurrences } = setup()
    await waitFor(() => expect(occurrences).toHaveBeenCalledTimes(1))
    const { result } = renderHook(hook as () => { mutateAsync: (a: unknown) => Promise<unknown> }, { wrapper })

    await result.current.mutateAsync(arg)

    await waitFor(() => expect(occurrences).toHaveBeenCalledTimes(2))
  })

  it.each([
    ['create', () => useCreateSubscriptionLink('s1'), { label: 'x', sources: [], onlyMine: false }],
    ['revoke', () => useRevokeSubscriptionLink('s1'), 'l1'],
  ] as const)('reads only the addresses again after %s', async (_name, hook, arg) => {
    const { wrapper, occurrences, links } = setup()
    await waitFor(() => expect(links).toHaveBeenCalledTimes(1))
    const { result } = renderHook(hook as () => { mutateAsync: (a: unknown) => Promise<unknown> }, { wrapper })

    await result.current.mutateAsync(arg)

    await waitFor(() => expect(links).toHaveBeenCalledTimes(2))
    expect(occurrences).toHaveBeenCalledTimes(1)
  })
})
