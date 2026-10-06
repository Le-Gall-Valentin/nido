import { client } from '@/shared/api'
import { linkGone, toApiError, WeakPasswordError } from '@/shared/lib'
import type { IPasswordResetApi } from '../model/IPasswordResetApi'

export const passwordResetApi: IPasswordResetApi = {
  async requestReset(identifier: string): Promise<void> {
    try {
      await client.post('/auth/password-reset/request', { identifier })
    } catch (error) {
      throw toApiError(error)
    }
  },

  async checkToken(token: string): Promise<void> {
    try {
      await client.post('/auth/password-reset/check', { token })
    } catch (error) {
      throw toApiError(error, linkGone)
    }
  },

  async confirmReset(token: string, newPassword: string): Promise<void> {
    try {
      await client.post('/auth/password-reset/confirm', { token, newPassword })
    } catch (error) {
      throw toApiError(error, (status) => linkGone(status) ?? (status === 400 ? new WeakPasswordError() : null))
    }
  },
}
