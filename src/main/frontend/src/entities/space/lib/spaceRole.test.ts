import { describe, it, expect } from 'vitest'
import { canManageSpace, canWrite, isOwner, rank } from './spaceRole'

describe('space role helpers', () => {
  it('lets owners and admins manage a space', () => {
    expect(canManageSpace('OWNER')).toBe(true)
    expect(canManageSpace('ADMIN')).toBe(true)
    expect(canManageSpace('MEMBER')).toBe(false)
    expect(canManageSpace('VIEWER')).toBe(false)
  })

  it('makes only the viewer read-only', () => {
    expect(canWrite('OWNER')).toBe(true)
    expect(canWrite('ADMIN')).toBe(true)
    expect(canWrite('MEMBER')).toBe(true)
    expect(canWrite('VIEWER')).toBe(false)
  })

  it('identifies the owner', () => {
    expect(isOwner('OWNER')).toBe(true)
    expect(isOwner('ADMIN')).toBe(false)
  })

  it('ranks roles by power, ascending', () => {
    expect(rank('VIEWER')).toBeLessThan(rank('MEMBER'))
    expect(rank('MEMBER')).toBeLessThan(rank('ADMIN'))
    expect(rank('ADMIN')).toBeLessThan(rank('OWNER'))
  })
})
