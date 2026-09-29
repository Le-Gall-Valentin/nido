import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { createTestQueryClient } from '@/shared/test'
import type { IPasswordResetApi } from './IPasswordResetApi'
import { usePasswordResetAvailability } from './usePasswordResetAvailability'

function api(capabilities: IPasswordResetApi['capabilities']): IPasswordResetApi {
  return { capabilities, requestReset: vi.fn(), checkToken: vi.fn(), confirmReset: vi.fn() }
}

function availabilityWith(fake: IPasswordResetApi) {
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
    const { result } = availabilityWith(api(async () => ({ passwordReset: true })))

    await waitFor(() => expect(result.current).toBe('available'))
  })

  it('is unavailable when the server says so', async () => {
    const { result } = availabilityWith(api(async () => ({ passwordReset: false })))

    await waitFor(() => expect(result.current).toBe('unavailable'))
  })

  it('a failed request counts as unavailable', async () => {
    const { result } = availabilityWith(api(async () => { throw new Error('network') }))

    await waitFor(() => expect(result.current).toBe('unavailable'))
  })
})
