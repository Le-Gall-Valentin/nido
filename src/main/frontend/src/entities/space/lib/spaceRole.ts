import type { SpaceRole } from '../model/types'

const RANKS: Record<SpaceRole, number> = { VIEWER: 0, MEMBER: 1, ADMIN: 2, OWNER: 3 }

export function rank(role: SpaceRole): number {
  return RANKS[role]
}

/** Gérer les membres, les invitations et l'identité du groupe. */
export function canManageSpace(role: SpaceRole): boolean {
  return role === 'OWNER' || role === 'ADMIN'
}

/** Écrire du contenu métier. Le VIEWER est en lecture seule. */
export function canWrite(role: SpaceRole): boolean {
  return role !== 'VIEWER'
}

export function isOwner(role: SpaceRole): boolean {
  return role === 'OWNER'
}
