export type SettingSource = 'ENVIRONMENT' | 'DATABASE' | 'DEFAULT'

export interface SettingField {
  key: string
  /** Never sent for a secret. */
  value: string | null
  source: SettingSource
  variable: string
  secret: boolean
  set: boolean
}

export interface SettingsGroup {
  group: 'mail' | 'public-url' | 'sessions' | 'api'
  fields: SettingField[]
}

export interface InstanceSettings {
  groups: SettingsGroup[]
}
