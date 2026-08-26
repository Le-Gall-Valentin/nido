import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError } from '@/shared/lib'
import type { SpaceDetail, SpaceSummary } from '@/entities/space'
import type { ISpacesApi } from '../model/ISpacesApi'

export class SpaceNotAccessibleError extends Error {
  constructor() { super('Space does not exist or is no longer accessible'); this.name = 'SpaceNotAccessibleError' }
}

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    // The backend deliberately returns 404 — not 403 — when the caller is not
    // a member of the space, so that a member and a stranger cannot tell each
    // other's spaces apart from the response. Translate it as "not
    // accessible" rather than a generic NotFoundError: this case is never a
    // permission problem to describe to the user.
    if (status === 404) throw new SpaceNotAccessibleError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const spacesApi: ISpacesApi = {
  async listMySpaces(): Promise<SpaceSummary[]> {
    try {
      const res = await client.get<SpaceSummary[]>('/spaces')
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async getSpace(spaceId: string): Promise<SpaceDetail> {
    try {
      const res = await client.get<SpaceDetail>(`/spaces/${spaceId}`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },
}
