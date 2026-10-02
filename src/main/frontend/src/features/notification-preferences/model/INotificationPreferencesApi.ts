import type { NotificationPreferences } from './types'

/**
 * The signed-in account's notification choices on the server. The card depends on this contract; the
 * axios implementation is injected (defaulting to notificationPreferencesApi), never imported by the UI.
 */
export interface INotificationPreferencesApi {
  get(): Promise<NotificationPreferences>
  /** Switches a whole channel; each kind keeps its own choice. */
  setChannel(channel: string, enabled: boolean): Promise<void>
  setType(type: string, enabled: boolean): Promise<void>
}
