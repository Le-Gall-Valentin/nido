// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import { passwordResetApi } from './passwordResetApi'
import { InvalidResetLinkError, WeakPasswordError } from '../model/errors'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn(), post: vi.fn() } }))

const mocked = client as unknown as { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> }

function axiosError(status?: number, headers: Record<string, string> = {}): AxiosError {
  return new axios.AxiosError('error', undefined, undefined, undefined, status === undefined ? undefined : {
    status, data: {}, headers, config: {} as never, statusText: String(status),
  })
}

describe('passwordResetApi', () => {
  beforeEach(() => {
    mocked.get.mockReset()
    mocked.post.mockReset()
  })

  it('asks what the sign-in pages may offer', async () => {
    mocked.get.mockResolvedValue({ data: { passwordReset: true, mail: true } })

    await expect(passwordResetApi.capabilities()).resolves.toEqual({ passwordReset: true, mail: true })
    expect(mocked.get).toHaveBeenCalledWith('/auth/capabilities')
  })

  it('sends the identifier as typed', async () => {
    mocked.post.mockResolvedValue({ status: 202 })

    await passwordResetApi.requestReset('jane')

    expect(mocked.post).toHaveBeenCalledWith('/auth/password-reset/request', { identifier: 'jane' })
  })

  it('reads a refusal to send more as a rate limit, with its delay', async () => {
    mocked.post.mockRejectedValue(axiosError(429, { 'retry-after': '42' }))

    const error = await passwordResetApi.requestReset('jane').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(RateLimitError)
    expect((error as RateLimitError).retryAfterSeconds).toBe(42)
  })

  it('reads 410 as a link that no longer works, on check and on confirm', async () => {
    mocked.post.mockRejectedValue(axiosError(410))

    await expect(passwordResetApi.checkToken('t')).rejects.toBeInstanceOf(InvalidResetLinkError)
    await expect(passwordResetApi.confirmReset('t', 'NewPassw0rd!')).rejects.toBeInstanceOf(InvalidResetLinkError)
    expect(mocked.post).toHaveBeenLastCalledWith('/auth/password-reset/confirm', { token: 't', newPassword: 'NewPassw0rd!' })
  })

  it('reads 400 on confirm as a password the rules refuse', async () => {
    mocked.post.mockRejectedValue(axiosError(400))

    await expect(passwordResetApi.confirmReset('t', 'weak')).rejects.toBeInstanceOf(WeakPasswordError)
  })

  it('reports other answers as server errors and no answer as a network error', async () => {
    mocked.post.mockRejectedValue(axiosError(500))
    await expect(passwordResetApi.checkToken('t')).rejects.toBeInstanceOf(ServerError)

    mocked.get.mockRejectedValue(axiosError())
    await expect(passwordResetApi.capabilities()).rejects.toBeInstanceOf(NetworkError)
  })
})
