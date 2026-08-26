import type { SpaceDetail, SpaceSummary } from '@/entities/space'

/**
 * Port for space context data. Consumers (hooks) depend on this contract,
 * never on the concrete axios-backed implementation, which is injected
 * through SpacesApiProvider.
 */
export interface ISpacesApi {
  listMySpaces(): Promise<SpaceSummary[]>
  getSpace(spaceId: string): Promise<SpaceDetail>
}
