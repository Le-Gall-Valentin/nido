import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import type { INotificationPreferencesApi } from './INotificationPreferencesApi'
import type { NotificationPreferences, PreferenceTarget } from './types'
import { NOTIFICATION_PREFERENCES_QUERY_KEY } from './useNotificationPreferences'
import { usePreferenceSwitches } from './usePreferenceSwitches'

const PREFERENCES: NotificationPreferences = {
  channels: [{ channel: 'email', enabled: true }],
  types: [{ type: 'space.invitation', enabled: true }],
}
const KIND: PreferenceTarget = { kind: 'type', code: 'space.invitation' }
const MAIL: PreferenceTarget = { kind: 'channel', code: 'email' }

function fakeApi(overrides: Partial<INotificationPreferencesApi> = {}): INotificationPreferencesApi {
  return {
    get: vi.fn().mockResolvedValue(PREFERENCES),
    setChannel: vi.fn().mockResolvedValue(undefined),
    setType: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

function renderSwitches(api: INotificationPreferencesApi) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  queryClient.setQueryData(NOTIFICATION_PREFERENCES_QUERY_KEY, PREFERENCES)
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return renderHook(() => usePreferenceSwitches(api), { wrapper })
}

describe('usePreferenceSwitches', () => {
  it('holds a switch while its request runs, and only that one', async () => {
    const { result } = renderSwitches(fakeApi({ setType: vi.fn(() => new Promise<void>(() => {})) }))

    act(() => { void result.current.change(KIND, false) })

    await waitFor(() => expect(result.current.isPending(KIND)).toBe(true))
    expect(result.current.isPending(MAIL)).toBe(false)
  })

  it('lets the switch go once the server has it', async () => {
    const { result } = renderSwitches(fakeApi())

    await act(() => result.current.change(KIND, false))

    expect(result.current.isPending(KIND)).toBe(false)
    expect(result.current.failure).toBeNull()
  })

  it.each([
    ['rate_limit', new RateLimitError()],
    ['network', new NetworkError()],
    ['server', new ServerError()],
  ] as const)('says why the server refused (%s)', async (failure, error) => {
    const { result } = renderSwitches(fakeApi({ setType: vi.fn().mockRejectedValue(error) }))

    await act(() => result.current.change(KIND, false))

    expect(result.current.failure).toBe(failure)
    expect(result.current.isPending(KIND)).toBe(false)
  })

  it('forgets the last failure at the next change', async () => {
    const setType = vi.fn().mockRejectedValueOnce(new NetworkError()).mockResolvedValue(undefined)
    const { result } = renderSwitches(fakeApi({ setType }))
    await act(() => result.current.change(KIND, false))
    expect(result.current.failure).toBe('network')

    await act(() => result.current.change(KIND, false))

    expect(result.current.failure).toBeNull()
  })
})
