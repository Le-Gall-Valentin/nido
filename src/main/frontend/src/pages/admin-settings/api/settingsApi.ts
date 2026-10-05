import { toInstanceError } from '@/entities/instance-settings'
import { client } from '@/shared/api'
import type { ISettingsApi } from '../model/ISettingsApi'
import type { InstanceSettings } from '@/entities/instance-settings'

export const settingsApi: ISettingsApi = {
  async get() {
    try {
      const { data } = await client.get<InstanceSettings>('/admin/settings')
      return data
    } catch (error) {
      throw toInstanceError(error)
    }
  },
  async update(group, values) {
    try {
      const { data } = await client.put<InstanceSettings>(`/admin/settings/${encodeURIComponent(group)}`, { values })
      return data
    } catch (error) {
      throw toInstanceError(error)
    }
  },
  async reset(group, key) {
    try {
      const { data } = await client.delete<InstanceSettings>(`/admin/settings/${encodeURIComponent(group)}/${encodeURIComponent(key)}`)
      return data
    } catch (error) {
      throw toInstanceError(error)
    }
  },
  async testMail(values) {
    try {
      await client.post('/admin/settings/mail/test', { values })
    } catch (error) {
      throw toInstanceError(error)
    }
  },
}
