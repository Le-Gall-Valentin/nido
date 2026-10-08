import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import type { User } from '@/entities/user'
import type { LoginApiResult, LoginCredentials, TwoFactorChallenge } from '../model/types'
import type { IAuthApi } from '../model/IAuthApi'
import { CredentialsError, NetworkError, RateLimitError, ServerError } from '../model/errors'
import { parseRetryAfter } from '@/shared/lib'

interface TwoFactorRequiredBody extends TwoFactorChallenge {
  twoFactorRequired: true
}

export const authApi: IAuthApi = {
  async login(credentials: LoginCredentials): Promise<LoginApiResult> {
    try {
      const { data } = await client.post<TwoFactorRequiredBody | User>('/auth/login', credentials)
      if ('twoFactorRequired' in data && data.twoFactorRequired === true) {
        const { username, methods, maskedEmail, mailCode } = data
        return { type: 'two_factor_required', challenge: { username, methods, maskedEmail: maskedEmail ?? null, mailCode: mailCode ?? null } }
      }
      return { type: 'success', user: data as User }
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) throw new CredentialsError()
        if (status === 429) throw new RateLimitError(parseRetryAfter(error.response?.headers))
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },

  async logout(): Promise<void> {
    try {
      await client.post('/auth/logout')
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status !== undefined && status >= 500) throw new ServerError()
        if (status !== undefined) return // 4xx → token already gone, treat as success
      }
      throw new NetworkError()
    }
  },

  async getMe(): Promise<User> {
    try {
      const { data } = await client.get<User>('/users/me')
      return data
    } catch (error) {
      if (isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) throw new CredentialsError()
        if (status !== undefined) throw new ServerError()
      }
      throw new NetworkError()
    }
  },
}