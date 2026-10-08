// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { twoFactorApi } from './twoFactorApi'
import { client } from '@/shared/api'
import {
  ChallengeExpiredError, CodeError, ConfirmMaxAttemptsError, EnrolmentExpiredError, MaxAttemptsError,
  CodeExpiredError, CodeSpentError, MethodAlreadyEnabledError, MethodNotEnabledError, MethodUnavailableError, ResendTooSoonError, SendLimitError,
} from '../model/errors'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

vi.mock('@/shared/api', () => ({
  client: { post: vi.fn(), get: vi.fn(), delete: vi.fn() },
}))

const mocked = client as unknown as {
  post: ReturnType<typeof vi.fn>
  get: ReturnType<typeof vi.fn>
  delete: ReturnType<typeof vi.fn>
}

function problem(status: number, data: Record<string, unknown> = {}, headers: Record<string, string> = {}): AxiosError {
  return new axios.AxiosError('error', undefined, undefined, undefined, {
    status, data, headers, config: {} as never, statusText: String(status),
  })
}

describe('twoFactorApi', () => {
  beforeEach(() => {
    mocked.post.mockReset()
    mocked.get.mockReset()
    mocked.delete.mockReset()
  })

  it('verifies a code with its method', async () => {
    mocked.post.mockResolvedValue({ data: { username: 'jane' } })

    await expect(twoFactorApi.verify('MAIL', '004213')).resolves.toEqual({ username: 'jane' })
    expect(mocked.post).toHaveBeenCalledWith('/auth/2fa/verify', { method: 'MAIL', code: '004213' })
  })

  it('tells a wrong code from an expired sign-in and from a lockout', async () => {
    mocked.post.mockRejectedValueOnce(problem(401))
    await expect(twoFactorApi.verify('APP', '000000')).rejects.toBeInstanceOf(CodeError)

    mocked.post.mockRejectedValueOnce(problem(401, { error_code: 'two_factor_challenge_expired' }))
    await expect(twoFactorApi.verify('APP', '000000')).rejects.toBeInstanceOf(ChallengeExpiredError)

    mocked.post.mockRejectedValueOnce(problem(429))
    await expect(twoFactorApi.verify('APP', '000000')).rejects.toBeInstanceOf(MaxAttemptsError)

    mocked.post.mockRejectedValueOnce(problem(429, {}, { 'retry-after': '12' }))
    await expect(twoFactorApi.verify('APP', '000000')).rejects.toEqual(new RateLimitError(12))
  })

  it('names a method that is off or paused', async () => {
    mocked.post.mockRejectedValueOnce(problem(409, { error_code: 'method_not_enabled' }))
    await expect(twoFactorApi.verify('MAIL', '000000')).rejects.toBeInstanceOf(MethodNotEnabledError)

    mocked.post.mockRejectedValueOnce(problem(409, { error_code: 'method_unavailable' }))
    await expect(twoFactorApi.verify('MAIL', '000000')).rejects.toBeInstanceOf(MethodUnavailableError)
  })

  it('asks for the sign-in code by mail and reads both refusals with their wait', async () => {
    mocked.post.mockResolvedValueOnce({ data: { sent: true, resendAfterSeconds: 60 } })
    await expect(twoFactorApi.sendMailCode()).resolves.toEqual({ resendAfterSeconds: 60 })
    expect(mocked.post).toHaveBeenCalledWith('/auth/2fa/challenge/mail')

    mocked.post.mockRejectedValueOnce(problem(429, { error_code: 'resend_too_soon', retryAfterSeconds: 42 }, { 'retry-after': '42' }))
    const tooSoon = await twoFactorApi.sendMailCode().catch((e: unknown) => e)
    expect(tooSoon).toBeInstanceOf(ResendTooSoonError)
    expect((tooSoon as ResendTooSoonError).seconds).toBe(42)

    mocked.post.mockRejectedValueOnce(problem(429, { error_code: 'send_limit_reached', retryAfterSeconds: 600 }, { 'retry-after': '600' }))
    const limit = await twoFactorApi.sendMailCode().catch((e: unknown) => e)
    expect(limit).toBeInstanceOf(SendLimitError)
    expect((limit as SendLimitError).seconds).toBe(600)
  })

  it('lists the methods of the account', async () => {
    const methods = [{ method: 'APP', enabled: true, usable: true }, { method: 'MAIL', enabled: false, usable: false }]
    mocked.get.mockResolvedValue({ data: methods })

    await expect(twoFactorApi.list()).resolves.toEqual(methods)
    expect(mocked.get).toHaveBeenCalledWith('/auth/2fa')
  })

  it('starts each method on its own route', async () => {
    mocked.post.mockResolvedValueOnce({ data: { otpauthUri: 'otpauth://x', secret: 'S' } })
    await expect(twoFactorApi.setupApp()).resolves.toEqual({ otpauthUri: 'otpauth://x', secret: 'S' })
    expect(mocked.post).toHaveBeenLastCalledWith('/auth/2fa/app/setup')

    mocked.post.mockResolvedValueOnce({ data: { sentTo: 'jane@example.fr', resendAfterSeconds: 60 } })
    await expect(twoFactorApi.setupMail()).resolves.toEqual({ sentTo: 'jane@example.fr', resendAfterSeconds: 60 })
    expect(mocked.post).toHaveBeenLastCalledWith('/auth/2fa/mail/setup')

    mocked.post.mockRejectedValueOnce(problem(409, { error_code: 'method_unavailable' }))
    await expect(twoFactorApi.setupMail()).rejects.toBeInstanceOf(MethodUnavailableError)

    mocked.post.mockRejectedValueOnce(problem(409, { error_code: 'method_already_enabled' }))
    await expect(twoFactorApi.setupApp()).rejects.toBeInstanceOf(MethodAlreadyEnabledError)
  })

  it('confirms a method and reads the end of an enrolment', async () => {
    mocked.post.mockResolvedValueOnce({})
    await twoFactorApi.confirm('MAIL', '004213')
    expect(mocked.post).toHaveBeenLastCalledWith('/auth/2fa/mail/confirm', { code: '004213' })

    mocked.post.mockRejectedValueOnce(problem(401))
    await expect(twoFactorApi.confirm('APP', '000000')).rejects.toBeInstanceOf(CodeError)

    mocked.post.mockRejectedValueOnce(problem(429))
    await expect(twoFactorApi.confirm('APP', '000000')).rejects.toBeInstanceOf(ConfirmMaxAttemptsError)

    mocked.post.mockRejectedValueOnce(problem(422))
    await expect(twoFactorApi.confirm('MAIL', '000000')).rejects.toBeInstanceOf(EnrolmentExpiredError)
  })

  it('turns a method off with its code, or without one when it is paused', async () => {
    mocked.delete.mockResolvedValue({})

    await twoFactorApi.disable('APP', '123456')
    expect(mocked.delete).toHaveBeenLastCalledWith('/auth/2fa/app', { data: { code: '123456' } })

    await twoFactorApi.disable('MAIL')
    expect(mocked.delete).toHaveBeenLastCalledWith('/auth/2fa/mail', {})
  })

  it('asks for the code that turns the mail off', async () => {
    mocked.post.mockResolvedValueOnce({ data: { resendAfterSeconds: 60 } })

    await expect(twoFactorApi.sendDisableCode()).resolves.toEqual({ resendAfterSeconds: 60 })
    expect(mocked.post).toHaveBeenLastCalledWith('/auth/2fa/mail/disable-code')
  })

  it('reads a server error and a lost network as such', async () => {
    mocked.get.mockRejectedValueOnce(problem(500))
    await expect(twoFactorApi.list()).rejects.toBeInstanceOf(ServerError)

    mocked.get.mockRejectedValueOnce(new Error('offline'))
    await expect(twoFactorApi.list()).rejects.toBeInstanceOf(NetworkError)
  })

  it('names a code that wrong guesses spent', async () => {
    mocked.delete.mockRejectedValueOnce(problem(410, { error_code: 'code_spent' }))

    await expect(twoFactorApi.disable('MAIL', '004213')).rejects.toBeInstanceOf(CodeSpentError)
  })

  it('turning a method off reads a wrong code, and a 429 without a wait as the lockout', async () => {
    mocked.delete.mockRejectedValueOnce(problem(401))
    await expect(twoFactorApi.disable('APP', '000000')).rejects.toBeInstanceOf(CodeError)

    mocked.delete.mockRejectedValueOnce(problem(429))
    await expect(twoFactorApi.disable('APP', '000000')).rejects.toBeInstanceOf(MaxAttemptsError)
  })

  it('outside sign-in and setup, a 429 without a wait has no lockout to name: a server error', async () => {
    mocked.get.mockRejectedValueOnce(problem(429))
    await expect(twoFactorApi.list()).rejects.toBeInstanceOf(ServerError)

    mocked.post.mockRejectedValueOnce(problem(429))
    await expect(twoFactorApi.setupApp()).rejects.toBeInstanceOf(ServerError)

    mocked.post.mockRejectedValueOnce(problem(429))
    await expect(twoFactorApi.setupMail()).rejects.toBeInstanceOf(ServerError)

    mocked.post.mockRejectedValueOnce(problem(429))
    await expect(twoFactorApi.sendDisableCode()).rejects.toBeInstanceOf(ServerError)
  })

  it('names a code no longer waiting apart from one wrong guesses spent', async () => {
    mocked.delete.mockRejectedValueOnce(problem(410, { error_code: 'code_expired' }))

    await expect(twoFactorApi.disable('MAIL', '004213')).rejects.toBeInstanceOf(CodeExpiredError)
  })

  it('a sign-in locked by wrong codes is a lockout, though it says when', async () => {
    // Read as a rate limit, the code screen would invite another try that the lockout refuses.
    mocked.post.mockRejectedValueOnce(problem(429, { error_code: 'two_factor_locked' }, { 'retry-after': '600' }))

    await expect(twoFactorApi.sendMailCode()).rejects.toBeInstanceOf(MaxAttemptsError)
  })
})
