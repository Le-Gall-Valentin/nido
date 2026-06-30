import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import type { User } from '@/entities/user'
import type { ITotpVerifyApi } from '../model/ITotpVerifyApi'
import type { ITotpEnrollApi } from '../model/ITotpEnrollApi'
import type { TotpSetupData } from '../model/types'
import { TotpAlreadyEnabledError, TotpChallengeExpiredError, TotpCodeError, TotpConfirmMaxAttemptsError, TotpDisableMaxAttemptsError, TotpMaxAttemptsError } from '../model/errors'
import { NetworkError, ServerError, RateLimitError, parseRetryAfter } from '@/shared/lib'

function handleTotpApiError(error: unknown, statusHandlers: Partial<Record<number, () => never>>): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status !== undefined) {
      const handler = statusHandlers[status]
      if (handler) handler()
      if (status === 429) {
        throw new RateLimitError(parseRetryAfter(error.response?.headers))
      }
      throw new ServerError()
    }
  }
  throw new NetworkError()
}

export const totpApi: ITotpVerifyApi & ITotpEnrollApi = {
  async verify(code: string): Promise<User> {
    try {
      const { data } = await client.post<User>('/auth/2fa/verify', { code })
      return data
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) {
          // error_code is a stable backend field (not tied to Java class names).
          // 'totp_challenge_expired' → TOTP session timed out, user must restart login.
          // absent                  → wrong or replayed code (catch-all).
          const errorCode = error.response?.data?.error_code as string | undefined
          if (errorCode === 'totp_challenge_expired') throw new TotpChallengeExpiredError()
          throw new TotpCodeError()
        }
        if (status === 429) {
          // retry-after present → global rate limiter; absent → TOTP attempt lockout.
          const retryAfterSeconds = parseRetryAfter(error.response?.headers)
          if (retryAfterSeconds === null) throw new TotpMaxAttemptsError()
          throw new RateLimitError(retryAfterSeconds)
        }
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async setup(): Promise<TotpSetupData> {
    try {
      const { data } = await client.post<TotpSetupData>('/auth/2fa/setup')
      return data
    } catch (error) {
      handleTotpApiError(error, {
        409: () => { throw new TotpAlreadyEnabledError() },
      })
    }
  },

  async confirm(code: string): Promise<void> {
    try {
      await client.post('/auth/2fa/confirm', { code })
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 429) {
        const retryAfter = parseRetryAfter(error.response.headers)
        if (retryAfter === null) throw new TotpConfirmMaxAttemptsError()
        throw new RateLimitError(retryAfter)
      }
      handleTotpApiError(error, {
        401: () => { throw new TotpCodeError() },
      })
    }
  },

  async getStatus(): Promise<{ totpEnabled: boolean }> {
    try {
      const { data } = await client.get<{ totpEnabled: boolean }>('/auth/2fa/status')
      return data
    } catch (error) {
      handleTotpApiError(error, {})
    }
  },

  async disable(code: string): Promise<void> {
    try {
      await client.delete('/auth/2fa', { data: { code } })
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) throw new TotpCodeError()
        if (status === 429) {
          const retryAfter = parseRetryAfter(error.response?.headers)
          if (retryAfter === null) throw new TotpDisableMaxAttemptsError()
          throw new RateLimitError(retryAfter)
        }
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },
}