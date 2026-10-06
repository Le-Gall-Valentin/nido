import { client } from '@/shared/api'
import { linkGone, toApiError, WeakPasswordError } from '@/shared/lib'
import type { IAccountInvitationApi } from '../model/IAccountInvitationApi'

export const accountInvitationApi: IAccountInvitationApi = {
  async checkInvitation(token: string): Promise<string> {
    try {
      const { data } = await client.post<{ username: string }>('/auth/account-invitation/check', { token })
      return data.username
    } catch (error) {
      throw toApiError(error, linkGone)
    }
  },

  async acceptInvitation(token: string, password: string): Promise<void> {
    try {
      await client.post('/auth/account-invitation/accept', { token, password })
    } catch (error) {
      throw toApiError(error, (status) => linkGone(status) ?? (status === 400 ? new WeakPasswordError() : null))
    }
  },
}
