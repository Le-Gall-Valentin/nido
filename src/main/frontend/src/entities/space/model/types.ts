export type SpaceType = 'PERSONAL' | 'SHARED'

/**
 * A role inside one context. Distinct from the platform role (`UserRole` in
 * `entities/user`): this one says what you may do inside a given context, the
 * other what you may do to accounts. The two never mix.
 */
export type SpaceRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'

export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REVOKED'

export interface SpaceSummary {
  id: string
  type: SpaceType
  name: string
  accent: string
  glyph: string
  myRole: SpaceRole
  memberCount: number
}

export interface SpaceDetail extends SpaceSummary {
  description: string | null
}

export interface SpaceMember {
  userId: string
  /** Null once the account has been anonymised by a GDPR deletion. */
  username: string | null
  /** Null once the account has been anonymised by a GDPR deletion. */
  email: string | null
  role: SpaceRole
  joinedAt: string
}

export interface SpaceInvitation {
  id: string
  email: string
  role: SpaceRole
  /** Clear text, only ever returned to the context's managers, who issued it. */
  code: string
  status: InvitationStatus
  expiresAt: string
  createdAt: string
}

export interface ReceivedInvitation {
  invitationId: string
  spaceId: string
  spaceName: string
  spaceAccent: string
  spaceGlyph: string
  role: SpaceRole
  expiresAt: string
}

export function isPersonal(space: Pick<SpaceSummary, 'type'>): boolean {
  return space.type === 'PERSONAL'
}
