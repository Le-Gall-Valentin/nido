import type { NotificationPreferences } from './types'

/**
 * The signed-in account's notification choices on the server. The card and its hooks take it as a
 * parameter, defaulting to the axios implementation (notificationPreferencesApi); tests pass a fake.
 */
export interface INotificationPreferencesApi {
  get(): Promise<NotificationPreferences>
  /** Switches a whole channel; each kind keeps its own choice. */
  setChannel(channel: string, enabled: boolean): Promise<void>
  setType(type: string, enabled: boolean): Promise<void>
}
