import type { User, UserRole, AdminUser } from '@/entities/user'

/**
 * Frontend mirror of the backend authorization rules
 * (identity/application/handler/* + shared/service/RoleHierarchy.java).
 * Keep both sides in sync: the backend is the source of truth, these
 * checks only exist to disable actions before they would fail.
 */

export type PermissionDenialReason =
  | 'super_admin_protected'
  | 'self'
  | 'admin_cannot_manage_admin'
  | 'insufficient'
  | 'totp_not_enabled'
  | 'target_inactive'
  | 'no_assignable_role'

export type PermissionResult =
  | { ok: true }
  | { ok: false; reason: PermissionDenialReason }

const OK: PermissionResult = { ok: true }

function deny(reason: PermissionDenialReason): PermissionResult {
  return { ok: false, reason }
}

/** Mirror of RoleHierarchy.canManage: SUPER_ADMIN → {ADMIN, USER}, ADMIN → {USER}. */
export function canManage(callerRole: UserRole, targetRole: UserRole): boolean {
  if (callerRole === 'SUPER_ADMIN') return targetRole !== 'SUPER_ADMIN'
  if (callerRole === 'ADMIN') return targetRole === 'USER'
  return false
}

/** Roles the caller is allowed to assign or create (RoleHierarchy.canManage on the new role). */
export function assignableRoles(callerRole: UserRole): Array<'USER' | 'ADMIN'> {
  if (callerRole === 'SUPER_ADMIN') return ['USER', 'ADMIN']
  if (callerRole === 'ADMIN') return ['USER']
  return []
}

function hierarchyDenialReason(caller: User, target: AdminUser): PermissionDenialReason {
  if (target.role === 'SUPER_ADMIN') return 'super_admin_protected'
  if (caller.role === 'USER') return 'insufficient'
  return 'admin_cannot_manage_admin'
}

/** DeactivateUserHandler: self → hierarchy. */
export function canDeactivate(caller: User, target: AdminUser): PermissionResult {
  if (caller.id === target.id) return deny('self')
  if (!canManage(caller.role, target.role)) return deny(hierarchyDenialReason(caller, target))
  return OK
}

/** ActivateUserHandler: self → hierarchy. */
export function canActivate(caller: User, target: AdminUser): PermissionResult {
  if (caller.id === target.id) return deny('self')
  if (!canManage(caller.role, target.role)) return deny(hierarchyDenialReason(caller, target))
  return OK
}

/** DeleteUserHandler: self → hierarchy. */
export function canDelete(caller: User, target: AdminUser): PermissionResult {
  if (caller.id === target.id) return deny('self')
  if (!canManage(caller.role, target.role)) return deny(hierarchyDenialReason(caller, target))
  return OK
}

/**
 * AdminResetTotpHandler: self → hierarchy.
 * The totpEnabled check is frontend-only UX (backend treats reset of a
 * disabled TOTP as a no-op).
 */
export function canResetTotp(caller: User, target: AdminUser): PermissionResult {
  if (caller.id === target.id) return deny('self')
  if (!canManage(caller.role, target.role)) return deny(hierarchyDenialReason(caller, target))
  if (!target.totpEnabled) return deny('totp_not_enabled')
  return OK
}

/** UpdateUserHandler: self → target active → hierarchy on current role. */
export function canEditRole(caller: User, target: AdminUser): PermissionResult {
  if (caller.id === target.id) return deny('self')
  if (!target.isActive) return deny('target_inactive')
  if (!canManage(caller.role, target.role)) return deny(hierarchyDenialReason(caller, target))
  // The role can only change if the caller can assign a role other than the
  // target's current one. An ADMIN may only assign USER, so a USER target is a
  // dead end — mirror of UpdateUserHandler rejecting a no-op role change.
  if (!assignableRoles(caller.role).some(r => r !== target.role)) return deny('no_assignable_role')
  return OK
}
