import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import type { User } from '@/entities/user'
import type { LoginApiResult, LoginCredentials } from '../model/types'
import type { IAuthApi } from '../model/IAuthApi'
import { CredentialsError, NetworkError, RateLimitError, ServerError } from '../model/errors'
import { parseRetryAfter } from '@/shared/lib'

export const authApi: IAuthApi = {
  async login(credentials: LoginCredentials): Promise<LoginApiResult> {
    try {
      const { data } = await client.post<{ totpRequired?: true } | User>('/auth/login', credentials)
      if ('totpRequired' in data && data.totpRequired === true) return { type: 'totp_required' }
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