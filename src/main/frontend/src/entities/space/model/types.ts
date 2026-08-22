export type SpaceType = 'PERSONAL' | 'SHARED'

/**
 * Rôle dans un contexte. Distinct du rôle de plateforme (`UserRole` dans
 * `entities/user`) : celui-ci dit ce qu'on peut faire dans un contexte donné,
 * l'autre ce qu'on peut faire sur les comptes. Les deux ne se croisent jamais.
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
  /** Null quand le compte a été anonymisé par une suppression RGPD. */
  username: string | null
  /** Null quand le compte a été anonymisé par une suppression RGPD. */
  email: string | null
  role: SpaceRole
  joinedAt: string
}

export interface SpaceInvitation {
  id: string
  email: string
  role: SpaceRole
  /** En clair, réservé aux gestionnaires du contexte, qui l'ont émis. */
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
