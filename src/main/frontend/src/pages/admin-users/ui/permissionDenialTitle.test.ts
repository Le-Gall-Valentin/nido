import { describe, it, expect } from 'vitest'
import { permissionDenialTitle } from './permissionDenialTitle'

const echo = (key: string) => key

describe('permissionDenialTitle', () => {
  it('returns the fallback when the action is allowed', () => {
    expect(permissionDenialTitle({ ok: true }, echo, 'Delete')).toBe('Delete')
  })

  it('returns the localized reason key when denied', () => {
    expect(permissionDenialTitle({ ok: false, reason: 'self' }, echo, 'Delete')).toBe('perm.self')
    expect(permissionDenialTitle({ ok: false, reason: 'super_admin_protected' }, echo, 'Delete'))
      .toBe('perm.super_admin_protected')
  })
})
