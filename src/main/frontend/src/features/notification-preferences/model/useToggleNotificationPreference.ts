import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notificationPreferencesApi } from '../api/notificationPreferencesApi'
import type { INotificationPreferencesApi } from './INotificationPreferencesApi'
import { applyChange } from './preferenceChange'
import type { NotificationPreferences, PreferenceChange } from './types'
import { NOTIFICATION_PREFERENCES_QUERY_KEY } from './useNotificationPreferences'

/**
 * One switch, shown at once. On failure only that switch goes back — not a snapshot of the whole card,
 * which would also undo another switch flipped meanwhile. Nothing is refetched on success: the server
 * holds exactly what the card shows.
 */
export function useToggleNotificationPreference(api: INotificationPreferencesApi = notificationPreferencesApi) {
  const queryClient = useQueryClient()
  const update = (change: PreferenceChange) =>
    queryClient.setQueryData<NotificationPreferences>(NOTIFICATION_PREFERENCES_QUERY_KEY,
      (current) => current && applyChange(current, change))

  return useMutation({
    mutationFn: ({ target, enabled }: PreferenceChange) =>
      target.kind === 'channel' ? api.setChannel(target.code, enabled) : api.setType(target.code, enabled),
    onMutate: async (change: PreferenceChange) => {
      await queryClient.cancelQueries({ queryKey: NOTIFICATION_PREFERENCES_QUERY_KEY })
      update(change)
    },
    onError: (_error, change) => {
      update({ ...change, enabled: !change.enabled })
    },
  })
}
