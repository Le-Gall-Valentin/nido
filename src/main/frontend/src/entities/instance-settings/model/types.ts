export type SettingSource = 'ENVIRONMENT' | 'DATABASE' | 'DEFAULT'

export interface SettingField {
  key: string
  /** Never sent for a secret. */
  value: string | null
  source: SettingSource
  variable: string
  secret: boolean
  /** Never emptied: no way back to a default it does not have. */
  required: boolean
  set: boolean
}

export type SettingsGroupCode = 'mail' | 'public-url' | 'sessions' | 'api'

export interface SettingsGroup {
  group: SettingsGroupCode
  fields: SettingField[]
}

export interface InstanceSettings {
  groups: SettingsGroup[]
}
