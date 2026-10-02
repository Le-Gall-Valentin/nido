import { useQuery } from '@tanstack/react-query'
import { notificationPreferencesApi } from '../api/notificationPreferencesApi'
import type { INotificationPreferencesApi } from './INotificationPreferencesApi'

export const NOTIFICATION_PREFERENCES_QUERY_KEY = ['notification-preferences'] as const

export function useNotificationPreferences(api: INotificationPreferencesApi = notificationPreferencesApi) {
  return useQuery({
    queryKey: NOTIFICATION_PREFERENCES_QUERY_KEY,
    queryFn: () => api.get(),
  })
}
