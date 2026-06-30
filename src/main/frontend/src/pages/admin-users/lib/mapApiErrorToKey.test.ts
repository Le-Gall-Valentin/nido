import { describe, it, expect } from 'vitest'
import { mapApiErrorToKey } from './mapApiErrorToKey'
import { ConflictError, RoleAlreadyAssignedError } from '../api/adminUsersApi'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'

describe('mapApiErrorToKey', () => {
  it('maps ConflictError', () => {
    expect(mapApiErrorToKey(new ConflictError(), 'create')).toBe('create.error.conflict')
  })

  it('maps RoleAlreadyAssignedError', () => {
    expect(mapApiErrorToKey(new RoleAlreadyAssignedError(), 'edit_role')).toBe('edit_role.error.already_assigned')
  })

  it('maps ForbiddenError', () => {
    expect(mapApiErrorToKey(new ForbiddenError(), 'delete')).toBe('delete.error.forbidden')
  })

  it('maps NotFoundError', () => {
    expect(mapApiErrorToKey(new NotFoundError(), 'reset_totp')).toBe('reset_totp.error.not_found')
  })

  it('maps RateLimitError', () => {
    expect(mapApiErrorToKey(new RateLimitError(), 'delete')).toBe('delete.error.rate_limit')
  })

  it('maps NetworkError', () => {
    expect(mapApiErrorToKey(new NetworkError(), 'reset_totp')).toBe('reset_totp.error.network')
  })

  it('maps ServerError and unknown errors to server', () => {
    expect(mapApiErrorToKey(new ServerError(), 'create')).toBe('create.error.server')
    expect(mapApiErrorToKey(new Error('boom'), 'create')).toBe('create.error.server')
  })
})
