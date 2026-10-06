import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { createTestQueryClient } from '@/shared/test'
import type { ICapabilitiesApi } from './ICapabilitiesApi'
import { useMailAvailability, usePasswordResetAvailability } from './useCapabilities'

function api(capabilities: ICapabilitiesApi['capabilities']): ICapabilitiesApi {
  return { capabilities }
}

function availabilityWith(fake: ICapabilitiesApi) {
  const client = createTestQueryClient()
  return renderHook(() => usePasswordResetAvailability(fake), {
    wrapper: ({ children }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>,
  })
}

describe('usePasswordResetAvailability', () => {
  it('is loading until the server answers', () => {
    const { result } = availabilityWith(api(() => new Promise(() => {})))

    expect(result.current).toBe('loading')
  })

  it('is available when the server says so', async () => {
    const { result } = availabilityWith(api(async () => ({ passwordReset: true, mail: true })))

    await waitFor(() => expect(result.current).toBe('available'))
  })

  it('is unavailable when the server says so', async () => {
    const { result } = availabilityWith(api(async () => ({ passwordReset: false, mail: false })))

    await waitFor(() => expect(result.current).toBe('unavailable'))
  })

  it('a failed request counts as unavailable', async () => {
    const { result } = availabilityWith(api(async () => { throw new Error('network') }))

    await waitFor(() => expect(result.current).toBe('unavailable'))
  })

  it('asks again on the next visit, since mail can be switched on from the settings page', async () => {
    const capabilities = vi.fn()
      .mockResolvedValueOnce({ passwordReset: false, mail: false })
      .mockResolvedValueOnce({ passwordReset: true, mail: true })
    // A client that keeps its cache, as the application's does: without it, a second visit asks
    // again whatever the hook says.
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const wrapper = ({ children }: { children: React.ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>

    const first = renderHook(() => usePasswordResetAvailability(api(capabilities)), { wrapper })
    await waitFor(() => expect(first.result.current).toBe('unavailable'))
    first.unmount()

    const second = renderHook(() => usePasswordResetAvailability(api(capabilities)), { wrapper })
    await waitFor(() => expect(second.result.current).toBe('available'))
  })
})

describe('useMailAvailability', () => {
  it('reads whether mail is configured from the same single answer', async () => {
    const capabilities = vi.fn(async () => ({ passwordReset: false, mail: true }))
    const client = createTestQueryClient()
    const { result } = renderHook(
      () => ({ mail: useMailAvailability(api(capabilities)), reset: usePasswordResetAvailability(api(capabilities)) }),
      { wrapper: ({ children }) => <QueryClientProvider client={client}>{children}</QueryClientProvider> },
    )

    await waitFor(() => expect(result.current.mail).toBe('available'))
    expect(result.current.reset).toBe('unavailable')
    expect(capabilities).toHaveBeenCalledTimes(1)
  })
})
