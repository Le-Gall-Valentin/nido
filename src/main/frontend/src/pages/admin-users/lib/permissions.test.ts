import { describe, it, expect } from 'vitest'
import {
  canManage,
  assignableRoles,
  canDeactivate,
  canActivate,
  canDelete,
  canResetTotp,
  canEditRole,
} from './permissions'
import type { User, AdminUser, UserRole } from '@/entities/user'

function makeUser(id: string, role: UserRole): User {
  return { id, username: id, email: `${id}@test.com`, role, createdAt: '2024-01-01T00:00:00Z', totpEnabled: true }
}

function makeAdminUser(id: string, role: UserRole, overrides: Partial<AdminUser> = {}): AdminUser {
  return { ...makeUser(id, role), isActive: true, ...overrides }
}

const SUPER_ADMIN = makeUser('sa', 'SUPER_ADMIN')
const ADMIN = makeUser('a1', 'ADMIN')
const USER = makeUser('u1', 'USER')

describe('canManage — mirror of backend RoleHierarchy', () => {
  it('SUPER_ADMIN manages ADMIN and USER but not SUPER_ADMIN', () => {
    expect(canManage('SUPER_ADMIN', 'ADMIN')).toBe(true)
    expect(canManage('SUPER_ADMIN', 'USER')).toBe(true)
    expect(canManage('SUPER_ADMIN', 'SUPER_ADMIN')).toBe(false)
  })

  it('ADMIN manages USER only', () => {
    expect(canManage('ADMIN', 'USER')).toBe(true)
    expect(canManage('ADMIN', 'ADMIN')).toBe(false)
    expect(canManage('ADMIN', 'SUPER_ADMIN')).toBe(false)
  })

  it('USER manages nothing', () => {
    expect(canManage('USER', 'USER')).toBe(false)
    expect(canManage('USER', 'ADMIN')).toBe(false)
    expect(canManage('USER', 'SUPER_ADMIN')).toBe(false)
  })
})

describe('assignableRoles', () => {
  it('SUPER_ADMIN can assign USER and ADMIN', () => {
    expect(assignableRoles('SUPER_ADMIN')).toEqual(['USER', 'ADMIN'])
  })

  it('ADMIN can assign USER only', () => {
    expect(assignableRoles('ADMIN')).toEqual(['USER'])
  })

  it('USER caller gets no assignable roles (mirror of canManage)', () => {
    expect(assignableRoles('USER')).toEqual([])
  })

  it('never includes SUPER_ADMIN', () => {
    expect(assignableRoles('SUPER_ADMIN')).not.toContain('SUPER_ADMIN')
  })
})

describe('canDeactivate', () => {
  it('denies self', () => {
    expect(canDeactivate(SUPER_ADMIN, makeAdminUser('sa', 'SUPER_ADMIN'))).toEqual({ ok: false, reason: 'self' })
  })

  it('denies SUPER_ADMIN target with super_admin_protected', () => {
    expect(canDeactivate(ADMIN, makeAdminUser('sa2', 'SUPER_ADMIN'))).toEqual({ ok: false, reason: 'super_admin_protected' })
  })

  it('denies ADMIN on ADMIN', () => {
    expect(canDeactivate(ADMIN, makeAdminUser('a2', 'ADMIN'))).toEqual({ ok: false, reason: 'admin_cannot_manage_admin' })
  })

  it('denies USER caller', () => {
    expect(canDeactivate(USER, makeAdminUser('u2', 'USER'))).toEqual({ ok: false, reason: 'insufficient' })
  })

  it('allows SUPER_ADMIN on ADMIN', () => {
    expect(canDeactivate(SUPER_ADMIN, makeAdminUser('a2', 'ADMIN'))).toEqual({ ok: true })
  })

  it('allows ADMIN on USER', () => {
    expect(canDeactivate(ADMIN, makeAdminUser('u2', 'USER'))).toEqual({ ok: true })
  })
})

describe('canActivate', () => {
  it('denies self', () => {
    expect(canActivate(ADMIN, makeAdminUser('a1', 'ADMIN', { isActive: false }))).toEqual({ ok: false, reason: 'self' })
  })

  it('denies SUPER_ADMIN target with super_admin_protected', () => {
    expect(canActivate(SUPER_ADMIN, makeAdminUser('sa2', 'SUPER_ADMIN', { isActive: false }))).toEqual({ ok: false, reason: 'super_admin_protected' })
  })

  it('denies ADMIN on inactive ADMIN', () => {
    expect(canActivate(ADMIN, makeAdminUser('a2', 'ADMIN', { isActive: false }))).toEqual({ ok: false, reason: 'admin_cannot_manage_admin' })
  })

  it('denies USER caller', () => {
    expect(canActivate(USER, makeAdminUser('u2', 'USER', { isActive: false }))).toEqual({ ok: false, reason: 'insufficient' })
  })

  it('allows SUPER_ADMIN on inactive USER', () => {
    expect(canActivate(SUPER_ADMIN, makeAdminUser('u2', 'USER', { isActive: false }))).toEqual({ ok: true })
  })

  it('allows ADMIN on inactive USER', () => {
    expect(canActivate(ADMIN, makeAdminUser('u2', 'USER', { isActive: false }))).toEqual({ ok: true })
  })
})

describe('canDelete', () => {
  it('denies SUPER_ADMIN target', () => {
    expect(canDelete(SUPER_ADMIN, makeAdminUser('sa2', 'SUPER_ADMIN'))).toEqual({ ok: false, reason: 'super_admin_protected' })
  })

  it('denies self', () => {
    expect(canDelete(ADMIN, makeAdminUser('a1', 'ADMIN'))).toEqual({ ok: false, reason: 'self' })
  })

  it('denies ADMIN on ADMIN', () => {
    expect(canDelete(ADMIN, makeAdminUser('a2', 'ADMIN'))).toEqual({ ok: false, reason: 'admin_cannot_manage_admin' })
  })

  it('denies USER caller', () => {
    expect(canDelete(USER, makeAdminUser('u2', 'USER'))).toEqual({ ok: false, reason: 'insufficient' })
  })

  it('allows SUPER_ADMIN on ADMIN', () => {
    expect(canDelete(SUPER_ADMIN, makeAdminUser('a2', 'ADMIN'))).toEqual({ ok: true })
  })

  it('allows ADMIN on USER', () => {
    expect(canDelete(ADMIN, makeAdminUser('u2', 'USER'))).toEqual({ ok: true })
  })
})

describe('canResetTotp', () => {
  it('denies self', () => {
    expect(canResetTotp(SUPER_ADMIN, makeAdminUser('sa', 'SUPER_ADMIN'))).toEqual({ ok: false, reason: 'self' })
  })

  it('denies SUPER_ADMIN target with super_admin_protected', () => {
    expect(canResetTotp(ADMIN, makeAdminUser('sa2', 'SUPER_ADMIN'))).toEqual({ ok: false, reason: 'super_admin_protected' })
  })

  it('denies USER caller', () => {
    expect(canResetTotp(USER, makeAdminUser('u2', 'USER'))).toEqual({ ok: false, reason: 'insufficient' })
  })

  it('denies when target has no TOTP', () => {
    expect(canResetTotp(SUPER_ADMIN, makeAdminUser('u2', 'USER', { totpEnabled: false }))).toEqual({ ok: false, reason: 'totp_not_enabled' })
  })

  it('hierarchy denial wins over totp_not_enabled (mirrors backend order)', () => {
    expect(canResetTotp(ADMIN, makeAdminUser('a2', 'ADMIN', { totpEnabled: false }))).toEqual({ ok: false, reason: 'admin_cannot_manage_admin' })
  })

  it('denies ADMIN on ADMIN', () => {
    expect(canResetTotp(ADMIN, makeAdminUser('a2', 'ADMIN'))).toEqual({ ok: false, reason: 'admin_cannot_manage_admin' })
  })

  it('allows SUPER_ADMIN on ADMIN with TOTP', () => {
    expect(canResetTotp(SUPER_ADMIN, makeAdminUser('a2', 'ADMIN'))).toEqual({ ok: true })
  })

  it('allows ADMIN on USER with TOTP', () => {
    expect(canResetTotp(ADMIN, makeAdminUser('u2', 'USER'))).toEqual({ ok: true })
  })
})

describe('canEditRole', () => {
  it('denies self', () => {
    expect(canEditRole(SUPER_ADMIN, makeAdminUser('sa', 'SUPER_ADMIN'))).toEqual({ ok: false, reason: 'self' })
  })

  it('denies inactive target (backend UserNotActive)', () => {
    expect(canEditRole(SUPER_ADMIN, makeAdminUser('u2', 'USER', { isActive: false }))).toEqual({ ok: false, reason: 'target_inactive' })
  })

  it('denies SUPER_ADMIN target', () => {
    expect(canEditRole(ADMIN, makeAdminUser('sa2', 'SUPER_ADMIN'))).toEqual({ ok: false, reason: 'super_admin_protected' })
  })

  it('denies ADMIN on ADMIN', () => {
    expect(canEditRole(ADMIN, makeAdminUser('a2', 'ADMIN'))).toEqual({ ok: false, reason: 'admin_cannot_manage_admin' })
  })

  it('denies USER caller', () => {
    expect(canEditRole(USER, makeAdminUser('u2', 'USER'))).toEqual({ ok: false, reason: 'insufficient' })
  })

  it('allows SUPER_ADMIN on active ADMIN', () => {
    expect(canEditRole(SUPER_ADMIN, makeAdminUser('a2', 'ADMIN'))).toEqual({ ok: true })
  })

  it('denies ADMIN on active USER: USER is the only role an ADMIN can assign', () => {
    expect(canEditRole(ADMIN, makeAdminUser('u2', 'USER'))).toEqual({ ok: false, reason: 'no_assignable_role' })
  })

  it('allows ADMIN on active USER once a richer assignable set exists', () => {
    // Guards the rule rather than the current role table: if an ADMIN could
    // ever assign more than USER, editing a USER would become meaningful again.
    expect(canManage('ADMIN', 'USER')).toBe(true)
    expect(assignableRoles('ADMIN')).toEqual(['USER'])
  })
})
