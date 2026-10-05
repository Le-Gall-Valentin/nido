// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError } from '@/shared/lib'
import { accountInvitationApi } from './accountInvitationApi'
import { InvalidResetLinkError, WeakPasswordError } from '../model/errors'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn(), post: vi.fn() } }))

const mocked = client as unknown as { post: ReturnType<typeof vi.fn> }

function axiosError(status?: number): AxiosError {
  return new axios.AxiosError('error', undefined, undefined, undefined, status === undefined ? undefined : {
    status, data: {}, headers: {}, config: {} as never, statusText: String(status),
  })
}

describe('accountInvitationApi', () => {
  beforeEach(() => mocked.post.mockReset())

  it('learns whose invitation a link is', async () => {
    mocked.post.mockResolvedValue({ data: { username: 'carol' } })

    await expect(accountInvitationApi.checkInvitation('abc')).resolves.toBe('carol')
    expect(mocked.post).toHaveBeenCalledWith('/auth/account-invitation/check', { token: 'abc' })
  })

  it('sends the first password with the token', async () => {
    mocked.post.mockResolvedValue({ status: 204 })

    await accountInvitationApi.acceptInvitation('abc', 'Welcome-Home-1')

    expect(mocked.post).toHaveBeenCalledWith('/auth/account-invitation/accept', { token: 'abc', password: 'Welcome-Home-1' })
  })

  it('reads 410 as a link that no longer works, on check and on accept', async () => {
    mocked.post.mockRejectedValueOnce(axiosError(410)).mockRejectedValueOnce(axiosError(410))

    await expect(accountInvitationApi.checkInvitation('abc')).rejects.toBeInstanceOf(InvalidResetLinkError)
    await expect(accountInvitationApi.acceptInvitation('abc', 'Welcome-Home-1')).rejects.toBeInstanceOf(InvalidResetLinkError)
  })

  it('reads 400 on accept as a password the rules refuse', async () => {
    mocked.post.mockRejectedValueOnce(axiosError(400))

    await expect(accountInvitationApi.acceptInvitation('abc', 'weak')).rejects.toBeInstanceOf(WeakPasswordError)
  })

  it('reads the other failures as for any request', async () => {
    mocked.post.mockRejectedValueOnce(axiosError(429)).mockRejectedValueOnce(axiosError())

    await expect(accountInvitationApi.checkInvitation('abc')).rejects.toBeInstanceOf(RateLimitError)
    await expect(accountInvitationApi.checkInvitation('abc')).rejects.toBeInstanceOf(NetworkError)
  })
})
