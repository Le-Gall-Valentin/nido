import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { IDashboardApi } from '../model/IDashboardApi'
import type { Dashboard } from '../model/types'

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    if (status === 404) throw new NotFoundError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const dashboardApi: IDashboardApi = {
  async getDashboard(spaceId) {
    try {
      const res = await client.get<Dashboard>(`/spaces/${spaceId}/dashboard`)
      return res.data
    } catch (error) { handleError(error) }
  },
}
