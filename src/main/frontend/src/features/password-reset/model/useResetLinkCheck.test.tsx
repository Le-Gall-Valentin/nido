import { StrictMode, type ReactNode } from 'react'
import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import { InvalidLinkError, NetworkError } from '@/shared/lib'
import { createTestQueryClient } from '@/shared/test'
import type { IPasswordResetApi } from './IPasswordResetApi'
import { useResetLinkCheck } from './useResetLinkCheck'

function apiWith(checkToken: IPasswordResetApi['checkToken']): IPasswordResetApi {
  return { requestReset: vi.fn(), checkToken, confirmReset: vi.fn() }
}

function check(token: string | null, api: IPasswordResetApi, strict = false) {
  const client = createTestQueryClient()
  const wrapper = ({ children }: { children: ReactNode }) => {
    const tree = <QueryClientProvider client={client}>{children}</QueryClientProvider>
    return strict ? <StrictMode>{tree}</StrictMode> : tree
  }
  return renderHook(() => useResetLinkCheck(token, api), { wrapper })
}

describe('useResetLinkCheck', () => {
  it('calls a missing token invalid without asking the server', () => {
    const api = apiWith(vi.fn())
    const { result } = check(null, api)

    expect(result.current.state).toBe('invalid')
    expect(api.checkToken).not.toHaveBeenCalled()
  })

  it('is checking, then valid when the server accepts the link', async () => {
    const { result } = check('abc', apiWith(vi.fn().mockResolvedValue(undefined)))

    expect(result.current.state).toBe('checking')
    await waitFor(() => expect(result.current.state).toBe('valid'))
  })

  it('is invalid when the server says the link no longer works', async () => {
    const { result } = check('abc', apiWith(vi.fn().mockRejectedValue(new InvalidLinkError())))

    await waitFor(() => expect(result.current.state).toBe('invalid'))
  })

  it('is unavailable when the server could not be asked, and asks again on retry', async () => {
    const checkToken = vi.fn().mockRejectedValueOnce(new NetworkError()).mockResolvedValueOnce(undefined)
    const { result } = check('abc', apiWith(checkToken))
    await waitFor(() => expect(result.current.state).toBe('unavailable'))

    act(() => result.current.retry())

    await waitFor(() => expect(result.current.state).toBe('valid'))
    expect(checkToken).toHaveBeenCalledTimes(2)
  })

  it('asks the server once, even when React mounts twice to find side effects', async () => {
    const checkToken = vi.fn().mockResolvedValue(undefined)
    const { result } = check('abc', apiWith(checkToken), true)

    await waitFor(() => expect(result.current.state).toBe('valid'))
    expect(checkToken).toHaveBeenCalledTimes(1)
  })
})
