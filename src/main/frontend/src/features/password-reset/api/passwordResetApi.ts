import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, parseRetryAfter, RateLimitError, ServerError } from '@/shared/lib'
import type { IPasswordResetApi, PasswordResetCapabilities } from '../model/IPasswordResetApi'
import { InvalidResetLinkError, WeakPasswordError } from '../model/errors'

function toError(error: unknown, specific: (status: number) => Error | null = () => null): Error {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status !== undefined) {
      const known = specific(status)
      if (known) return known
      if (status === 429) return new RateLimitError(parseRetryAfter(error.response?.headers))
      return new ServerError()
    }
  }
  return new NetworkError()
}

const linkNoLongerWorks = (status: number) => (status === 410 ? new InvalidResetLinkError() : null)

export const passwordResetApi: IPasswordResetApi = {
  async capabilities(): Promise<PasswordResetCapabilities> {
    try {
      const { data } = await client.get<PasswordResetCapabilities>('/auth/capabilities')
      return data
    } catch (error) {
      throw toError(error)
    }
  },

  async requestReset(identifier: string): Promise<void> {
    try {
      await client.post('/auth/password-reset/request', { identifier })
    } catch (error) {
      throw toError(error)
    }
  },

  async checkToken(token: string): Promise<void> {
    try {
      await client.post('/auth/password-reset/check', { token })
    } catch (error) {
      throw toError(error, linkNoLongerWorks)
    }
  },

  async confirmReset(token: string, newPassword: string): Promise<void> {
    try {
      await client.post('/auth/password-reset/confirm', { token, newPassword })
    } catch (error) {
      throw toError(error, (status) => linkNoLongerWorks(status) ?? (status === 400 ? new WeakPasswordError() : null))
    }
  },
}
