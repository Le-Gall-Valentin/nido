import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import type { TwoFactorMethod, User } from '@/entities/user'
import { NetworkError, RateLimitError, ServerError, parseRetryAfter } from '@/shared/lib'
import type { ITwoFactorChallengeApi } from '../model/ITwoFactorChallengeApi'
import type { ITwoFactorMethodsApi } from '../model/ITwoFactorMethodsApi'
import type { AppSetupData, MailSetupData, MethodState, ResendData } from '../model/types'
import {
  ChallengeExpiredError, CodeError, ConfirmMaxAttemptsError, EnrolmentExpiredError, MaxAttemptsError,
  MethodAlreadyEnabledError, MethodNotEnabledError, MethodUnavailableError, ResendTooSoonError, SendLimitError,
} from '../model/errors'

/**
 * Every 2FA route answers in the same words: `error_code` names the case when there is one (stable, not tied to
 * Java class names); otherwise 401 is a wrong code, 422 an enrolment that is not there, and a 429 without
 * Retry-After the lockout that guards the code — with one, the route's rate limit.
 */
function fail(error: unknown, lockout: () => Error): never {
  if (!isAxiosError(error) || error.response === undefined) throw new NetworkError()
  const { status, headers } = error.response
  const data = (error.response.data ?? {}) as { error_code?: string; retryAfterSeconds?: number }
  const retryAfter = parseRetryAfter(headers)
  switch (data.error_code) {
    case 'two_factor_challenge_expired': throw new ChallengeExpiredError()
    case 'method_unavailable': throw new MethodUnavailableError()
    case 'method_not_enabled': throw new MethodNotEnabledError()
    case 'method_already_enabled': throw new MethodAlreadyEnabledError()
    case 'resend_too_soon': throw new ResendTooSoonError(data.retryAfterSeconds ?? retryAfter ?? 60)
    case 'send_limit_reached': throw new SendLimitError(data.retryAfterSeconds ?? retryAfter ?? 900)
  }
  if (status === 401) throw new CodeError()
  if (status === 422) throw new EnrolmentExpiredError()
  if (status === 429) {
    if (retryAfter === null) throw lockout()
    throw new RateLimitError(retryAfter)
  }
  throw new ServerError()
}

const routeOf = (method: TwoFactorMethod) => `/auth/2fa/${method.toLowerCase()}`

export const twoFactorApi: ITwoFactorChallengeApi & ITwoFactorMethodsApi = {
  async verify(method, code): Promise<User> {
    try {
      const { data } = await client.post<User>('/auth/2fa/verify', { method, code })
      return data
    } catch (error) {
      fail(error, () => new MaxAttemptsError())
    }
  },

  async sendMailCode(): Promise<ResendData> {
    try {
      const { data } = await client.post<{ resendAfterSeconds: number }>('/auth/2fa/challenge/mail')
      return { resendAfterSeconds: data.resendAfterSeconds }
    } catch (error) {
      fail(error, () => new MaxAttemptsError())
    }
  },

  async list(): Promise<MethodState[]> {
    try {
      const { data } = await client.get<MethodState[]>('/auth/2fa')
      return data
    } catch (error) {
      fail(error, () => new ServerError())
    }
  },

  async setupApp(): Promise<AppSetupData> {
    try {
      const { data } = await client.post<AppSetupData>(`${routeOf('APP')}/setup`)
      return data
    } catch (error) {
      fail(error, () => new ServerError())
    }
  },

  async setupMail(): Promise<MailSetupData> {
    try {
      const { data } = await client.post<MailSetupData>(`${routeOf('MAIL')}/setup`)
      return data
    } catch (error) {
      fail(error, () => new ServerError())
    }
  },

  async confirm(method, code): Promise<void> {
    try {
      await client.post(`${routeOf(method)}/confirm`, { code })
    } catch (error) {
      fail(error, () => new ConfirmMaxAttemptsError())
    }
  },

  async sendDisableCode(): Promise<ResendData> {
    try {
      const { data } = await client.post<ResendData>(`${routeOf('MAIL')}/disable-code`)
      return data
    } catch (error) {
      fail(error, () => new ServerError())
    }
  },

  async disable(method, code): Promise<void> {
    try {
      await client.delete(routeOf(method), code === undefined ? {} : { data: { code } })
    } catch (error) {
      fail(error, () => new MaxAttemptsError())
    }
  },
}
