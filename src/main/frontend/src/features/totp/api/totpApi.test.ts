// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { totpApi } from './totpApi'
import { client } from '@/shared/api'
import { TotpCodeError, TotpChallengeExpiredError, TotpAlreadyEnabledError, TotpMaxAttemptsError, TotpConfirmMaxAttemptsError, TotpDisableMaxAttemptsError } from '../model/errors'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

vi.mock('@/shared/api', () => ({
  client: {
    post: vi.fn(),
    get: vi.fn(),
    delete: vi.fn(),
  },
}))

const mockedClient = client as unknown as {
  post: ReturnType<typeof vi.fn>
  get: ReturnType<typeof vi.fn>
  delete: ReturnType<typeof vi.fn>
}

function makeAxiosError(
  status: number,
  message = 'error',
  headers: Record<string, string> = {},
  data: Record<string, unknown> = {},
): AxiosError {
  return new axios.AxiosError(message, undefined, undefined, undefined, {
    status,
    data,
    headers,
    config: {} as never,
    statusText: String(status),
  })
}

describe('totpApi', () => {
  beforeEach(() => {
    mockedClient.post.mockReset()
    mockedClient.get.mockReset()
    mockedClient.delete.mockReset()
  })

  // verify
  describe('verify', () => {
    it('returns User on success', async () => {
      const user = { id: '1', username: 'user', role: 'USER' }
      mockedClient.post.mockResolvedValue({ data: user })

      await expect(totpApi.verify('123456')).resolves.toEqual(user)
      expect(mockedClient.post).toHaveBeenCalledWith('/auth/2fa/verify', { code: '123456' })
    })

    it('throws TotpCodeError on 401 without error_code', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(401, 'Unauthorized', {}, { title: 'AuthenticationError' }))
      await expect(totpApi.verify('000000')).rejects.toBeInstanceOf(TotpCodeError)
    })

    it('throws TotpChallengeExpiredError on 401 with error_code totp_challenge_expired', async () => {
      mockedClient.post.mockRejectedValue(
        makeAxiosError(401, 'Unauthorized', {}, { title: 'AuthenticationError', error_code: 'totp_challenge_expired' }),
      )
      await expect(totpApi.verify('123456')).rejects.toBeInstanceOf(TotpChallengeExpiredError)
    })

    it('throws TotpMaxAttemptsError on 429 without retry-after header', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429, 'Too Many Requests'))
      await expect(totpApi.verify('123456')).rejects.toBeInstanceOf(TotpMaxAttemptsError)
    })

    it('throws RateLimitError on 429 with retry-after header', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429, 'Too Many Requests', { 'retry-after': '30' }))
      const caught = await totpApi.verify('123456').catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBe(30)
    })

    it('throws ServerError on 500', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(500))
      await expect(totpApi.verify('123456')).rejects.toBeInstanceOf(ServerError)
    })

    it('throws NetworkError on no response', async () => {
      mockedClient.post.mockRejectedValue(new Error('Network Error'))
      await expect(totpApi.verify('123456')).rejects.toBeInstanceOf(NetworkError)
    })
  })

  // setup
  describe('setup', () => {
    it('throws RateLimitError on 429', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429, 'Too Many Requests', { 'retry-after': '60' }))
      const caught = await totpApi.setup().catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBe(60)
    })

    it('throws RateLimitError with null retryAfterSeconds when header absent on 429', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429))
      const caught = await totpApi.setup().catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBeNull()
    })

    it('returns TotpSetupData on success', async () => {
      const setupData = { otpauthUri: 'otpauth://totp/App:user?secret=ABC', secret: 'ABC' }
      mockedClient.post.mockResolvedValue({ data: setupData })

      await expect(totpApi.setup()).resolves.toEqual(setupData)
      expect(mockedClient.post).toHaveBeenCalledWith('/auth/2fa/setup')
    })

    it('throws TotpAlreadyEnabledError on 409', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(409))
      await expect(totpApi.setup()).rejects.toBeInstanceOf(TotpAlreadyEnabledError)
    })

    it('throws ServerError on 500', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(500))
      await expect(totpApi.setup()).rejects.toBeInstanceOf(ServerError)
    })

    it('throws NetworkError on no response', async () => {
      mockedClient.post.mockRejectedValue(new Error('Network Error'))
      await expect(totpApi.setup()).rejects.toBeInstanceOf(NetworkError)
    })
  })

  // getStatus
  describe('getStatus', () => {
    it('throws RateLimitError on 429', async () => {
      mockedClient.get.mockRejectedValue(makeAxiosError(429, 'Too Many Requests', { 'retry-after': '10' }))
      const caught = await totpApi.getStatus().catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBe(10)
    })

    it('throws RateLimitError with null retryAfterSeconds when retry-after header absent', async () => {
      mockedClient.get.mockRejectedValue(makeAxiosError(429))
      const caught = await totpApi.getStatus().catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBeNull()
    })

    it('returns totpEnabled status on success', async () => {
      mockedClient.get.mockResolvedValue({ data: { totpEnabled: true } })

      await expect(totpApi.getStatus()).resolves.toEqual({ totpEnabled: true })
      expect(mockedClient.get).toHaveBeenCalledWith('/auth/2fa/status')
    })

    it('throws ServerError on 500', async () => {
      mockedClient.get.mockRejectedValue(makeAxiosError(500))
      await expect(totpApi.getStatus()).rejects.toBeInstanceOf(ServerError)
    })

    it('throws NetworkError on no response', async () => {
      mockedClient.get.mockRejectedValue(new Error('Network Error'))
      await expect(totpApi.getStatus()).rejects.toBeInstanceOf(NetworkError)
    })
  })

  // confirm
  describe('confirm', () => {
    it('throws TotpConfirmMaxAttemptsError on 429 without retry-after header', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429, 'Too Many Requests'))
      await expect(totpApi.confirm('000000')).rejects.toBeInstanceOf(TotpConfirmMaxAttemptsError)
    })

    it('throws TotpConfirmMaxAttemptsError on 429 even when body is absent', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429, 'Too Many Requests', {}, {}))
      await expect(totpApi.confirm('000000')).rejects.toBeInstanceOf(TotpConfirmMaxAttemptsError)
    })

    it('throws RateLimitError on 429 with retry-after header (global rate limiter)', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(429, 'Too Many Requests', { 'retry-after': '30' }))
      const caught = await totpApi.confirm('123456').catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBe(30)
    })

    it('resolves void on 204', async () => {
      mockedClient.post.mockResolvedValue({ status: 204, data: undefined })

      await expect(totpApi.confirm('123456')).resolves.toBeUndefined()
      expect(mockedClient.post).toHaveBeenCalledWith('/auth/2fa/confirm', { code: '123456' })
    })

    it('throws TotpCodeError on 401', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(401))
      await expect(totpApi.confirm('000000')).rejects.toBeInstanceOf(TotpCodeError)
    })

    it('throws ServerError on 500', async () => {
      mockedClient.post.mockRejectedValue(makeAxiosError(500))
      await expect(totpApi.confirm('123456')).rejects.toBeInstanceOf(ServerError)
    })

    it('throws NetworkError on no response', async () => {
      mockedClient.post.mockRejectedValue(new Error('Network Error'))
      await expect(totpApi.confirm('123456')).rejects.toBeInstanceOf(NetworkError)
    })
  })

  // disable
  describe('disable', () => {
    it('resolves void on 204', async () => {
      mockedClient.delete.mockResolvedValue({ status: 204, data: undefined })
      await expect(totpApi.disable('123456')).resolves.toBeUndefined()
      expect(mockedClient.delete).toHaveBeenCalledWith('/auth/2fa', { data: { code: '123456' } })
    })

    it('throws TotpCodeError on 401', async () => {
      mockedClient.delete.mockRejectedValue(makeAxiosError(401))
      await expect(totpApi.disable('000000')).rejects.toBeInstanceOf(TotpCodeError)
    })

    it('throws TotpDisableMaxAttemptsError on 429 without retry-after', async () => {
      mockedClient.delete.mockRejectedValue(makeAxiosError(429))
      await expect(totpApi.disable('000000')).rejects.toBeInstanceOf(TotpDisableMaxAttemptsError)
    })

    it('throws RateLimitError on 429 with retry-after', async () => {
      mockedClient.delete.mockRejectedValue(makeAxiosError(429, 'Too Many Requests', { 'retry-after': '30' }))
      const caught = await totpApi.disable('123456').catch((e) => e)
      expect(caught).toBeInstanceOf(RateLimitError)
      expect((caught as RateLimitError).retryAfterSeconds).toBe(30)
    })

    it('throws ServerError on 500', async () => {
      mockedClient.delete.mockRejectedValue(makeAxiosError(500))
      await expect(totpApi.disable('123456')).rejects.toBeInstanceOf(ServerError)
    })

    it('throws NetworkError on no response', async () => {
      mockedClient.delete.mockRejectedValue(new Error('Network Error'))
      await expect(totpApi.disable('123456')).rejects.toBeInstanceOf(NetworkError)
    })
  })
})