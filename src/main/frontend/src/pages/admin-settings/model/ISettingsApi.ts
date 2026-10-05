import type { InstanceSettings, SettingsGroupCode } from '@/entities/instance-settings'

export interface ISettingsApi {
  get(): Promise<InstanceSettings>
  update(group: SettingsGroupCode, values: Record<string, string>): Promise<InstanceSettings>
  reset(group: SettingsGroupCode, key: string): Promise<InstanceSettings>
  testMail(values: Record<string, string>): Promise<void>
}
