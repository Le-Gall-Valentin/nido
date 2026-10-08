import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, parseRetryAfter } from '@/shared/lib'
import { codeRefusalOf } from '@/features/two-factor'
import type { IAccountApi, ProfileUpdateResult } from '../model/IAccountApi'

export class ConflictError extends Error {
  constructor() { super('Username or email already taken'); this.name = 'ConflictError' }
}

export class InvalidCurrentPasswordError extends Error {
  constructor() { super('Invalid current password'); this.name = 'InvalidCurrentPasswordError' }
}

/** Wrong guesses took the code for the new address with them: a new one has to be asked for. */
export class EmailCodeSpentError extends Error {
  constructor() { super('The code for the new address no longer works'); this.name = 'EmailCodeSpentError' }
}

/** The code given for the new address is not the one sent there — not the session, not the password. */
export class EmailCodeInvalidError extends Error {
  constructor() { super('The code for the new address is invalid'); this.name = 'EmailCodeInvalidError' }
}

/** No code is waiting for the new address any more — never asked for, or past its ten minutes: ask for a new one. */
export class EmailCodeExpiredError extends Error {
  constructor() { super('No code is waiting for the new address'); this.name = 'EmailCodeExpiredError' }
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
      const response = await client.patch<{ sentTo: string; resendAfterSeconds: number; mailMethodRemoved?: boolean }>('/users/me', body)
      if (response.status === 202) {
        return { kind: 'email_code_sent', sentTo: response.data.sentTo, resendAfterSeconds: response.data.resendAfterSeconds }
      }
      // 200 rather than 204: saved while mail was off, and the code by mail removed with the old address.
      return { kind: 'saved', mailMethodRemoved: response.status === 200 && response.data.mailMethodRemoved === true }
    } catch (error) {
      if (isAxiosError(error) && error.response !== undefined) {
        const { status } = error.response
        const data = (error.response.data ?? {}) as { error_code?: string }
        const refusal = codeRefusalOf(error.response)
        if (refusal) throw refusal
        if (data.error_code === 'email_code_invalid') throw new EmailCodeInvalidError()
        if (data.error_code === 'email_code_expired') throw new EmailCodeExpiredError()
        if (data.error_code === 'email_code_spent') throw new EmailCodeSpentError()
        if (status === 409) throw new ConflictError()
        if (status === 422) throw new InvalidCurrentPasswordError()
        if (status === 429) throw new RateLimitError(parseRetryAfter(error.response.headers))
        throw new ServerError()
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