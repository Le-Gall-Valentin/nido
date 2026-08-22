import { describe, it, expect } from 'vitest'
import { mapSpaceErrorToKey } from './mapSpaceErrorToKey'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import { SpaceNotAccessibleError } from '@/features/space-switcher'
import {
  InsufficientRoleError,
  SelfManagementError,
  OwnerProtectedError,
  SpaceRoleAlreadyAssignedError,
  LastOwnerError,
  SpaceNotEmptyError,
  AlreadyMemberError,
  InvitationAlreadyPendingError,
  InvitationNotFoundError,
  InvitationNotPendingError,
  InvitationExpiredError,
  PersonalSpaceImmutableError,
  NoAccountForEmailError,
} from '../api/spacesPageApi'

describe('mapSpaceErrorToKey', () => {
  it('maps SpaceNotAccessibleError (not found / not a member, indistinguishable)', () => {
    expect(mapSpaceErrorToKey(new SpaceNotAccessibleError(), 'detail')).toBe('detail.error.not_accessible')
  })

  it('maps InsufficientRoleError', () => {
    expect(mapSpaceErrorToKey(new InsufficientRoleError(), 'invite')).toBe('invite.error.insufficient_role')
  })

  it('maps OwnerProtectedError', () => {
    expect(mapSpaceErrorToKey(new OwnerProtectedError(), 'remove_member')).toBe('remove_member.error.owner_protected')
  })

  it('maps SelfManagementError', () => {
    expect(mapSpaceErrorToKey(new SelfManagementError(), 'edit_role')).toBe('edit_role.error.self_management')
  })

  it('maps SpaceRoleAlreadyAssignedError', () => {
    expect(mapSpaceErrorToKey(new SpaceRoleAlreadyAssignedError(), 'edit_role')).toBe('edit_role.error.already_assigned')
  })

  it('maps LastOwnerError', () => {
    expect(mapSpaceErrorToKey(new LastOwnerError(), 'leave')).toBe('leave.error.last_owner')
  })

  it('maps SpaceNotEmptyError', () => {
    expect(mapSpaceErrorToKey(new SpaceNotEmptyError(), 'delete')).toBe('delete.error.not_empty')
  })

  it('maps PersonalSpaceImmutableError', () => {
    expect(mapSpaceErrorToKey(new PersonalSpaceImmutableError(), 'update')).toBe('update.error.personal_immutable')
  })

  it('maps NoAccountForEmailError', () => {
    expect(mapSpaceErrorToKey(new NoAccountForEmailError(), 'invite')).toBe('invite.error.no_account')
  })

  it('maps AlreadyMemberError', () => {
    expect(mapSpaceErrorToKey(new AlreadyMemberError(), 'invite')).toBe('invite.error.already_member')
  })

  it('maps InvitationAlreadyPendingError', () => {
    expect(mapSpaceErrorToKey(new InvitationAlreadyPendingError(), 'invite')).toBe('invite.error.invitation_pending')
  })

  it('maps InvitationNotFoundError', () => {
    expect(mapSpaceErrorToKey(new InvitationNotFoundError(), 'accept')).toBe('accept.error.invitation_not_found')
  })

  it('maps InvitationNotPendingError', () => {
    expect(mapSpaceErrorToKey(new InvitationNotPendingError(), 'revoke')).toBe('revoke.error.invitation_not_pending')
  })

  it('maps InvitationExpiredError', () => {
    expect(mapSpaceErrorToKey(new InvitationExpiredError(), 'accept')).toBe('accept.error.invitation_expired')
  })

  it('maps RateLimitError', () => {
    expect(mapSpaceErrorToKey(new RateLimitError(), 'create')).toBe('create.error.rate_limit')
  })

  it('maps NetworkError', () => {
    expect(mapSpaceErrorToKey(new NetworkError(), 'create')).toBe('create.error.network')
  })

  it('maps ServerError and unknown errors to server', () => {
    expect(mapSpaceErrorToKey(new ServerError(), 'create')).toBe('create.error.server')
    expect(mapSpaceErrorToKey(new Error('boom'), 'create')).toBe('create.error.server')
  })
})
