import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, parseRetryAfter, RateLimitError, ServerError } from '@/shared/lib'
import type { INotificationPreferencesApi } from '../model/INotificationPreferencesApi'
import type { NotificationPreferences } from '../model/types'

function toError(error: unknown): Error {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) return new RateLimitError(parseRetryAfter(error.response?.headers))
    if (status !== undefined) return new ServerError()
  }
  return new NetworkError()
}

export const notificationPreferencesApi: INotificationPreferencesApi = {
  async get(): Promise<NotificationPreferences> {
    try {
      const { data } = await client.get<NotificationPreferences>('/notifications/preferences')
      return data
    } catch (error) {
      throw toError(error)
    }
  },

  async setChannel(channel: string, enabled: boolean): Promise<void> {
    try {
      await client.put(`/notifications/preferences/channels/${encodeURIComponent(channel)}`, { enabled })
    } catch (error) {
      throw toError(error)
    }
  },

  async setType(type: string, enabled: boolean): Promise<void> {
    try {
      await client.put(`/notifications/preferences/types/${encodeURIComponent(type)}`, { enabled })
    } catch (error) {
      throw toError(error)
    }
  },
}
