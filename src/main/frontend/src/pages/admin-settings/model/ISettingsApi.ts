import type { InstanceSettings } from './types'

export interface ISettingsApi {
  get(): Promise<InstanceSettings>
  update(group: string, values: Record<string, string>): Promise<InstanceSettings>
  reset(group: string, key: string): Promise<InstanceSettings>
  testMail(values: Record<string, string>): Promise<void>
}
