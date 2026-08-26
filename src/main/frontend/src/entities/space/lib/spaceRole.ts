import type { SpaceRole } from '../model/types'

const RANKS: Record<SpaceRole, number> = { VIEWER: 0, MEMBER: 1, ADMIN: 2, OWNER: 3 }

export function rank(role: SpaceRole): number {
  return RANKS[role]
}

/** Manage members, invitations and the group's identity. */
export function canManageSpace(role: SpaceRole): boolean {
  return role === 'OWNER' || role === 'ADMIN'
}

/** Write business content. VIEWER is read-only. */
export function canWrite(role: SpaceRole): boolean {
  return role !== 'VIEWER'
}

export function isOwner(role: SpaceRole): boolean {
  return role === 'OWNER'
}
