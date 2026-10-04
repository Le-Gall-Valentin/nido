/** A channel this installation has (`email`), with the account's choice. */
export interface ChannelPreference {
  channel: string
  enabled: boolean
}

/** A kind of notification, `<context>.<name>` (`space.invitation`), with the account's choice. */
export interface TypePreference {
  type: string
  enabled: boolean
}

/** The card's content: no channel at all on an installation without mail. */
export interface NotificationPreferences {
  channels: ChannelPreference[]
  types: TypePreference[]
}

/** One switch of the card. */
export type PreferenceTarget = { kind: 'channel'; code: string } | { kind: 'type'; code: string }

export interface PreferenceChange {
  target: PreferenceTarget
  enabled: boolean
}
