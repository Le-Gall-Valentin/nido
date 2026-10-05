import { client } from '@/shared/api'
import type { IAccountInvitationApi } from '../model/IAccountInvitationApi'
import { WeakPasswordError } from '../model/errors'
import { linkNoLongerWorks, toError } from './apiErrors'

export const accountInvitationApi: IAccountInvitationApi = {
  async checkInvitation(token: string): Promise<string> {
    try {
      const { data } = await client.post<{ username: string }>('/auth/account-invitation/check', { token })
      return data.username
    } catch (error) {
      throw toError(error, linkNoLongerWorks)
    }
  },

  async acceptInvitation(token: string, password: string): Promise<void> {
    try {
      await client.post('/auth/account-invitation/accept', { token, password })
    } catch (error) {
      throw toError(error, (status) => linkNoLongerWorks(status) ?? (status === 400 ? new WeakPasswordError() : null))
    }
  },
}
