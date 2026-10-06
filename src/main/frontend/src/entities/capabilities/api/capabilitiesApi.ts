import { client } from '@/shared/api'
import { toApiError } from '@/shared/lib'
import type { Capabilities, ICapabilitiesApi } from '../model/ICapabilitiesApi'

export const capabilitiesApi: ICapabilitiesApi = {
  async capabilities(): Promise<Capabilities> {
    try {
      const { data } = await client.get<Capabilities>('/auth/capabilities')
      return data
    } catch (error) {
      throw toApiError(error)
    }
  },
}
