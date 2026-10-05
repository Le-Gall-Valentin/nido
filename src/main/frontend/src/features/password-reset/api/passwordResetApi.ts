import { client } from '@/shared/api'
import type { IPasswordResetApi, PasswordResetCapabilities } from '../model/IPasswordResetApi'
import { WeakPasswordError } from '../model/errors'
import { linkNoLongerWorks, toError } from './apiErrors'

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
