import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { createTestQueryClient } from '@/shared/test'
import { NetworkError } from '@/shared/lib'
import type { IAccountInvitationApi } from './IAccountInvitationApi'
import { InvalidResetLinkError } from './errors'
import { useInvitationLinkCheck } from './useInvitationLinkCheck'

function check(token: string | null, checkInvitation: IAccountInvitationApi['checkInvitation']) {
  const client = createTestQueryClient()
  return renderHook(() => useInvitationLinkCheck(token, { checkInvitation, acceptInvitation: vi.fn() }), {
    wrapper: ({ children }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>,
  })
}

describe('useInvitationLinkCheck', () => {
  it('is valid and names the account when the server knows the link', async () => {
    const { result } = check('abc', async () => 'carol')

    await waitFor(() => expect(result.current.state).toBe('valid'))
    expect(result.current.username).toBe('carol')
  })

  it('is invalid without a token, and asks nothing', () => {
    const checkInvitation = vi.fn()
    const { result } = check(null, checkInvitation)

    expect(result.current.state).toBe('invalid')
    expect(checkInvitation).not.toHaveBeenCalled()
  })

  it('is invalid when the server refuses the link', async () => {
    const { result } = check('abc', async () => { throw new InvalidResetLinkError() })

    await waitFor(() => expect(result.current.state).toBe('invalid'))
    expect(result.current.username).toBeNull()
  })

  it('is unavailable when the server could not be asked', async () => {
    const { result } = check('abc', async () => { throw new NetworkError() })

    await waitFor(() => expect(result.current.state).toBe('unavailable'))
  })
})
