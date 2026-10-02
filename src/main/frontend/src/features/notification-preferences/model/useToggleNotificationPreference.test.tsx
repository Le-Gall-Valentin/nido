import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { createTestQueryClient } from '@/shared/test'
import type { INotificationPreferencesApi } from './INotificationPreferencesApi'
import type { NotificationPreferences } from './types'
import { NOTIFICATION_PREFERENCES_QUERY_KEY, useNotificationPreferences } from './useNotificationPreferences'
import { useToggleNotificationPreference } from './useToggleNotificationPreference'

const PREFERENCES: NotificationPreferences = {
  channels: [{ channel: 'email', enabled: true }],
  types: [{ type: 'space.invitation', enabled: true }],
}

function fakeApi(overrides: Partial<INotificationPreferencesApi> = {}): INotificationPreferencesApi {
  return {
    get: vi.fn().mockResolvedValue(PREFERENCES),
    setChannel: vi.fn().mockResolvedValue(undefined),
    setType: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

/** Not createTestQueryClient: its gcTime 0 drops an entry without an observer before the test can read it. */
function seededClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: 0 }, mutations: { retry: false } },
  })
  queryClient.setQueryData(NOTIFICATION_PREFERENCES_QUERY_KEY, PREFERENCES)
  return queryClient
}

function wrapperFor(queryClient: QueryClient) {
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

const cached = (queryClient: QueryClient) =>
  queryClient.getQueryData<NotificationPreferences>(NOTIFICATION_PREFERENCES_QUERY_KEY)

describe('useNotificationPreferences', () => {
  it('reads the choices from the server', async () => {
    const api = fakeApi()
    const queryClient = createTestQueryClient()
    const { result } = renderHook(() => useNotificationPreferences(api), { wrapper: wrapperFor(queryClient) })

    await waitFor(() => expect(result.current.data).toEqual(PREFERENCES))
  })
})

describe('useToggleNotificationPreference', () => {
  it('shows the new state before the server answers', async () => {
    const api = fakeApi({ setType: vi.fn(() => new Promise<void>(() => {})) })
    const queryClient = seededClient()
    const { result } = renderHook(() => useToggleNotificationPreference(api), { wrapper: wrapperFor(queryClient) })

    result.current.mutate({ target: { kind: 'type', code: 'space.invitation' }, enabled: false })

    await waitFor(() => expect(cached(queryClient)?.types[0].enabled).toBe(false))
    expect(api.setType).toHaveBeenCalledWith('space.invitation', false)
  })

  it('keeps the new state once the server accepts it', async () => {
    const api = fakeApi()
    const queryClient = seededClient()
    const { result } = renderHook(() => useToggleNotificationPreference(api), { wrapper: wrapperFor(queryClient) })

    await result.current.mutateAsync({ target: { kind: 'channel', code: 'email' }, enabled: false })

    expect(cached(queryClient)?.channels[0].enabled).toBe(false)
  })

  it('puts back only the switch whose request failed', async () => {
    let failChannel!: (error: Error) => void
    const api = fakeApi({
      setChannel: vi.fn(() => new Promise<void>((_resolve, reject) => { failChannel = reject })),
    })
    const queryClient = seededClient()
    const { result } = renderHook(() => useToggleNotificationPreference(api), { wrapper: wrapperFor(queryClient) })

    const channelChange = result.current
      .mutateAsync({ target: { kind: 'channel', code: 'email' }, enabled: false })
      .catch(() => undefined)
    await result.current.mutateAsync({ target: { kind: 'type', code: 'space.invitation' }, enabled: false })
    await waitFor(() => expect(api.setChannel).toHaveBeenCalled())
    failChannel(new Error('boom'))
    await channelChange

    expect(cached(queryClient)?.channels[0].enabled).toBe(true)
    expect(cached(queryClient)?.types[0].enabled).toBe(false)
  })
})
