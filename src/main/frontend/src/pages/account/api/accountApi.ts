import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, parseRetryAfter } from '@/shared/lib'
import { ResendTooSoonError, SendLimitError } from '@/features/two-factor'
import type { IAccountApi, ProfileUpdateResult } from '../model/IAccountApi'

export class ConflictError extends Error {
  constructor() { super('Username or email already taken'); this.name = 'ConflictError' }
}

export class InvalidCurrentPasswordError extends Error {
  constructor() { super('Invalid current password'); this.name = 'InvalidCurrentPasswordError' }
}

/** The code given for the new address is not the one sent there, or no longer valid — not the session, not the password. */
export class EmailCodeInvalidError extends Error {
  constructor() { super('The code for the new address is invalid'); this.name = 'EmailCodeInvalidError' }
}

export const accountApi: IAccountApi = {
  async updateProfile(username: string, email: string, currentPassword?: string, emailCode?: string): Promise<ProfileUpdateResult> {
    try {
      const body = {
        username,
        email,
        ...(currentPassword === undefined ? {} : { currentPassword }),
        ...(emailCode === undefined ? {} : { emailCode }),
      }
      const response = await client.patch<{ sentTo: string; resendAfterSeconds: number }>('/users/me', body)
      if (response.status === 202) {
        return { kind: 'email_code_sent', sentTo: response.data.sentTo, resendAfterSeconds: response.data.resendAfterSeconds }
      }
      return { kind: 'saved' }
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        const data = (error.response?.data ?? {}) as { error_code?: string; retryAfterSeconds?: number }
        const retryAfter = parseRetryAfter(error.response?.headers)
        if (data.error_code === 'email_code_invalid') throw new EmailCodeInvalidError()
        if (data.error_code === 'resend_too_soon') throw new ResendTooSoonError(data.retryAfterSeconds ?? retryAfter ?? 60)
        if (data.error_code === 'send_limit_reached') throw new SendLimitError(data.retryAfterSeconds ?? retryAfter ?? 900)
        if (status === 409) throw new ConflictError()
        if (status === 422) throw new InvalidCurrentPasswordError()
        if (status === 429) throw new RateLimitError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    try {
      await client.patch('/users/me/password', { currentPassword, newPassword })
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 422) throw new InvalidCurrentPasswordError()
        if (status === 429) throw new RateLimitError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },
}