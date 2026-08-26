import type { SpaceDetail, SpaceMember, SpaceInvitation, ReceivedInvitation, SpaceRole } from '@/entities/space'

/** A role that can be assigned to a member or proposed in an invitation. OWNER is only reachable through a transfer. */
export type AssignableSpaceRole = Exclude<SpaceRole, 'OWNER'>

export interface CreateSpaceInput {
  name: string
  description?: string
  accent: string
  glyph: string
}

/** Partial update: a field absent from this object is left unchanged by the backend. */
export interface UpdateSpaceInput {
  name?: string
  description?: string
  accent?: string
  glyph?: string
}

/**
 * Port for the "Membres et groupes" page. Consumers (hooks) depend on this
 * contract, never on the concrete axios-backed implementation, which is
 * injected through SpacesPageApiProvider.
 */
export interface ISpacesPageApi {
  getSpaceDetail(spaceId: string): Promise<SpaceDetail>
  listMembers(spaceId: string): Promise<SpaceMember[]>
  listInvitations(spaceId: string): Promise<SpaceInvitation[]>
  listReceivedInvitations(): Promise<ReceivedInvitation[]>

  createSpace(input: CreateSpaceInput): Promise<SpaceDetail>
  updateSpace(spaceId: string, patch: UpdateSpaceInput): Promise<void>
  deleteSpace(spaceId: string): Promise<void>
  changeMemberRole(spaceId: string, userId: string, role: AssignableSpaceRole): Promise<void>
  removeMember(spaceId: string, userId: string): Promise<void>
  transferOwnership(spaceId: string, userId: string): Promise<void>
  leaveSpace(spaceId: string): Promise<void>
  inviteMember(spaceId: string, email: string, role: AssignableSpaceRole): Promise<SpaceInvitation>
  revokeInvitation(spaceId: string, invitationId: string): Promise<void>
  acceptInvitation(invitationId: string): Promise<{ spaceId: string }>
}
