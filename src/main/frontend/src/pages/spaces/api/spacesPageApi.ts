import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import { SpaceNotAccessibleError } from '@/features/space-switcher'
import type { SpaceDetail, SpaceMember, SpaceInvitation, ReceivedInvitation } from '@/entities/space'
import type {
  ISpacesPageApi,
  AssignableSpaceRole,
  CreateSpaceInput,
  UpdateSpaceInput,
} from '../model/ISpacesPageApi'

export { SpaceNotAccessibleError }

/** 403 InsufficientRole and 403 OwnerRequired: the caller's role does not allow this action. */
export class InsufficientRoleError extends Error {
  constructor() { super('Role does not allow this action'); this.name = 'InsufficientRoleError' }
}

/** 409 SelfManagementForbidden: acting on one's own membership through this operation. */
export class SelfManagementError extends Error {
  constructor() { super('Cannot manage your own membership'); this.name = 'SelfManagementError' }
}

/** 409 OwnerMembershipProtected: the target of the action is the space's owner. */
export class OwnerProtectedError extends Error {
  constructor() { super("The owner's membership is protected"); this.name = 'OwnerProtectedError' }
}

/** 409 RoleAlreadyAssigned: the member already has the requested role. */
export class SpaceRoleAlreadyAssignedError extends Error {
  constructor() { super('Member already has this role'); this.name = 'SpaceRoleAlreadyAssignedError' }
}

/** 409 LastOwnerCannotLeave: an owner must transfer ownership before leaving. */
export class LastOwnerError extends Error {
  constructor() { super('Transfer ownership before leaving this space'); this.name = 'LastOwnerError' }
}

/** 409 AlreadyMember: the invited address already belongs to this space. */
export class AlreadyMemberError extends Error {
  constructor() { super('This user is already a member'); this.name = 'AlreadyMemberError' }
}

/** 409 InvitationAlreadyPending: this address already has an outstanding invitation. */
export class InvitationAlreadyPendingError extends Error {
  constructor() { super('This address already has a pending invitation'); this.name = 'InvitationAlreadyPendingError' }
}

/** 404 InvitationNotFound: unknown, foreign, or already-consumed invitation id/code. */
export class InvitationNotFoundError extends Error {
  constructor() { super('Invitation not found'); this.name = 'InvitationNotFoundError' }
}

/**
 * 404 MemberNotFound: the target membership was removed (e.g. by another
 * manager) since the page loaded. Distinguishable from SpaceNotAccessibleError
 * so a stale row never tells the user the whole context vanished.
 */
export class MemberNotFoundError extends Error {
  constructor() { super('This member is no longer part of this space'); this.name = 'MemberNotFoundError' }
}

/** 409 InvitationNotPending: the invitation was already accepted or revoked. */
export class InvitationNotPendingError extends Error {
  constructor() { super('This invitation is no longer pending'); this.name = 'InvitationNotPendingError' }
}

/** 422 InvitationExpired. */
export class InvitationExpiredError extends Error {
  constructor() { super('This invitation has expired'); this.name = 'InvitationExpiredError' }
}

/** 422 PersonalSpaceImmutable: attempted rename/share/delete of a personal space. */
export class PersonalSpaceImmutableError extends Error {
  constructor() { super('The personal space cannot be renamed, shared or deleted'); this.name = 'PersonalSpaceImmutableError' }
}

/** 422 NoAccountForEmail: the invited address has no account on the platform. */
export class NoAccountForEmailError extends Error {
  constructor() { super('No account exists for this address'); this.name = 'NoAccountForEmailError' }
}

// Several distinct situations share HTTP 409 (and, for invitations, 422); the
// response body's `title` — the backend exception's class name — is the only
// way to tell them apart. See SpaceExceptionHandler.
const CONFLICT_TITLES: Record<string, () => never> = {
  SelfManagementForbidden: () => { throw new SelfManagementError() },
  OwnerMembershipProtected: () => { throw new OwnerProtectedError() },
  RoleAlreadyAssigned: () => { throw new SpaceRoleAlreadyAssignedError() },
  LastOwnerCannotLeave: () => { throw new LastOwnerError() },
  AlreadyMember: () => { throw new AlreadyMemberError() },
  InvitationAlreadyPending: () => { throw new InvitationAlreadyPendingError() },
  InvitationNotPending: () => { throw new InvitationNotPendingError() },
}

const UNPROCESSABLE_TITLES: Record<string, () => never> = {
  InvitationExpired: () => { throw new InvitationExpiredError() },
  PersonalSpaceImmutable: () => { throw new PersonalSpaceImmutableError() },
  NoAccountForEmail: () => { throw new NoAccountForEmailError() },
}

function titleOf(error: unknown): string | undefined {
  if (!isAxiosError(error)) return undefined
  const data = error.response?.data as { title?: unknown } | undefined
  return typeof data?.title === 'string' ? data.title : undefined
}

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new InsufficientRoleError()
    if (status === 404) {
      const title = titleOf(error)
      if (title === 'InvitationNotFound') throw new InvitationNotFoundError()
      if (title === 'MemberNotFound') throw new MemberNotFoundError()
      // Any other 404 (SpaceNotFound, NotAMember) is the backend's
      // deliberately indistinguishable "not accessible" response — never a
      // permission problem to describe to the user.
      throw new SpaceNotAccessibleError()
    }
    if (status === 409) {
      const title = titleOf(error)
      if (title && title in CONFLICT_TITLES) CONFLICT_TITLES[title]()
      throw new ServerError()
    }
    if (status === 422) {
      const title = titleOf(error)
      if (title && title in UNPROCESSABLE_TITLES) UNPROCESSABLE_TITLES[title]()
      throw new ServerError()
    }
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const spacesPageApi: ISpacesPageApi = {
  async getSpaceDetail(spaceId: string): Promise<SpaceDetail> {
    try {
      const res = await client.get<SpaceDetail>(`/spaces/${spaceId}`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async listMembers(spaceId: string): Promise<SpaceMember[]> {
    try {
      const res = await client.get<SpaceMember[]>(`/spaces/${spaceId}/members`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async listInvitations(spaceId: string): Promise<SpaceInvitation[]> {
    try {
      const res = await client.get<SpaceInvitation[]>(`/spaces/${spaceId}/invitations`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async listReceivedInvitations(): Promise<ReceivedInvitation[]> {
    try {
      const res = await client.get<ReceivedInvitation[]>('/me/invitations')
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async createSpace(input: CreateSpaceInput): Promise<SpaceDetail> {
    try {
      const res = await client.post<SpaceDetail>('/spaces', input)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async updateSpace(spaceId: string, patch: UpdateSpaceInput): Promise<void> {
    try {
      const body: Record<string, string> = {}
      if (patch.name !== undefined) body.name = patch.name
      if (patch.description !== undefined) body.description = patch.description
      if (patch.accent !== undefined) body.accent = patch.accent
      if (patch.glyph !== undefined) body.glyph = patch.glyph
      await client.patch(`/spaces/${spaceId}`, body)
    } catch (error) {
      handleError(error)
    }
  },

  async deleteSpace(spaceId: string): Promise<void> {
    try {
      await client.delete(`/spaces/${spaceId}`)
    } catch (error) {
      handleError(error)
    }
  },

  async changeMemberRole(spaceId: string, userId: string, role: AssignableSpaceRole): Promise<void> {
    try {
      await client.patch(`/spaces/${spaceId}/members/${userId}`, { role })
    } catch (error) {
      handleError(error)
    }
  },

  async removeMember(spaceId: string, userId: string): Promise<void> {
    try {
      await client.delete(`/spaces/${spaceId}/members/${userId}`)
    } catch (error) {
      handleError(error)
    }
  },

  async transferOwnership(spaceId: string, userId: string): Promise<void> {
    try {
      await client.post(`/spaces/${spaceId}/members/${userId}/ownership`)
    } catch (error) {
      handleError(error)
    }
  },

  async leaveSpace(spaceId: string): Promise<void> {
    try {
      await client.delete(`/spaces/${spaceId}/membership`)
    } catch (error) {
      handleError(error)
    }
  },

  async inviteMember(spaceId: string, email: string, role: AssignableSpaceRole): Promise<SpaceInvitation> {
    try {
      const res = await client.post<SpaceInvitation>(`/spaces/${spaceId}/invitations`, { email, role })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async revokeInvitation(spaceId: string, invitationId: string): Promise<void> {
    try {
      await client.delete(`/spaces/${spaceId}/invitations/${invitationId}`)
    } catch (error) {
      handleError(error)
    }
  },

  async acceptInvitation(invitationId: string): Promise<{ spaceId: string }> {
    try {
      const res = await client.post<{ spaceId: string }>(`/invitations/${invitationId}/accept`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },
}
