import { describe, it, expect } from 'vitest'
import { createInstance } from 'i18next'
import { mapSpaceErrorToKey } from './mapSpaceErrorToKey'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import { SpaceNotAccessibleError } from '@/features/space-switcher'
import {
  InsufficientRoleError,
  SelfManagementError,
  OwnerProtectedError,
  SpaceRoleAlreadyAssignedError,
  LastOwnerError,
  MemberNotFoundError,
  AlreadyMemberError,
  InvitationAlreadyPendingError,
  InvitationNotFoundError,
  InvitationNotPendingError,
  InvitationExpiredError,
  PersonalSpaceImmutableError,
  NoAccountForEmailError,
} from '../api/spacesPageApi'
import en from '../locales/en.json'
import fr from '../locales/fr.json'

describe('mapSpaceErrorToKey', () => {
  it('returns the operation-specific key followed by the shared fallback', () => {
    expect(mapSpaceErrorToKey(new SpaceNotAccessibleError(), 'detail')).toEqual([
      'detail.error.not_accessible',
      'errors.not_accessible',
    ])
  })

  it('maps InsufficientRoleError', () => {
    expect(mapSpaceErrorToKey(new InsufficientRoleError(), 'invite')).toEqual([
      'invite.error.insufficient_role',
      'errors.insufficient_role',
    ])
  })

  it('maps OwnerProtectedError', () => {
    expect(mapSpaceErrorToKey(new OwnerProtectedError(), 'members')).toEqual([
      'members.error.owner_protected',
      'errors.owner_protected',
    ])
  })

  it('maps SelfManagementError', () => {
    expect(mapSpaceErrorToKey(new SelfManagementError(), 'members')).toEqual([
      'members.error.self_management',
      'errors.self_management',
    ])
  })

  it('maps SpaceRoleAlreadyAssignedError', () => {
    expect(mapSpaceErrorToKey(new SpaceRoleAlreadyAssignedError(), 'members')).toEqual([
      'members.error.already_assigned',
      'errors.already_assigned',
    ])
  })

  it('maps LastOwnerError', () => {
    expect(mapSpaceErrorToKey(new LastOwnerError(), 'leave')).toEqual([
      'leave.error.last_owner',
      'errors.last_owner',
    ])
  })

  it('maps MemberNotFoundError', () => {
    expect(mapSpaceErrorToKey(new MemberNotFoundError(), 'members')).toEqual([
      'members.error.member_not_found',
      'errors.member_not_found',
    ])
  })

  it('maps PersonalSpaceImmutableError', () => {
    expect(mapSpaceErrorToKey(new PersonalSpaceImmutableError(), 'edit')).toEqual([
      'edit.error.personal_immutable',
      'errors.personal_immutable',
    ])
  })

  it('maps NoAccountForEmailError', () => {
    expect(mapSpaceErrorToKey(new NoAccountForEmailError(), 'invite')).toEqual([
      'invite.error.no_account',
      'errors.no_account',
    ])
  })

  it('maps AlreadyMemberError', () => {
    expect(mapSpaceErrorToKey(new AlreadyMemberError(), 'invite')).toEqual([
      'invite.error.already_member',
      'errors.already_member',
    ])
  })

  it('maps InvitationAlreadyPendingError', () => {
    expect(mapSpaceErrorToKey(new InvitationAlreadyPendingError(), 'invite')).toEqual([
      'invite.error.invitation_pending',
      'errors.invitation_pending',
    ])
  })

  it('maps InvitationNotFoundError', () => {
    expect(mapSpaceErrorToKey(new InvitationNotFoundError(), 'accept')).toEqual([
      'accept.error.invitation_not_found',
      'errors.invitation_not_found',
    ])
  })

  it('maps InvitationNotPendingError', () => {
    expect(mapSpaceErrorToKey(new InvitationNotPendingError(), 'invitations')).toEqual([
      'invitations.error.invitation_not_pending',
      'errors.invitation_not_pending',
    ])
  })

  it('maps InvitationExpiredError', () => {
    expect(mapSpaceErrorToKey(new InvitationExpiredError(), 'accept')).toEqual([
      'accept.error.invitation_expired',
      'errors.invitation_expired',
    ])
  })

  it('maps RateLimitError', () => {
    expect(mapSpaceErrorToKey(new RateLimitError(), 'create')).toEqual([
      'create.error.rate_limit',
      'errors.rate_limit',
    ])
  })

  it('maps NetworkError', () => {
    expect(mapSpaceErrorToKey(new NetworkError(), 'create')).toEqual([
      'create.error.network',
      'errors.network',
    ])
  })

  it('maps ServerError and unknown errors to server', () => {
    expect(mapSpaceErrorToKey(new ServerError(), 'create')).toEqual(['create.error.server', 'errors.server'])
    expect(mapSpaceErrorToKey(new Error('boom'), 'create')).toEqual(['create.error.server', 'errors.server'])
  })
})

describe('mapSpaceErrorToKey — every produced key resolves to a real translation', () => {
  // Every error type mapSpaceErrorToKey knows how to translate.
  const ERRORS: Record<string, unknown> = {
    SpaceNotAccessibleError: new SpaceNotAccessibleError(),
    MemberNotFoundError: new MemberNotFoundError(),
    InsufficientRoleError: new InsufficientRoleError(),
    OwnerProtectedError: new OwnerProtectedError(),
    SelfManagementError: new SelfManagementError(),
    SpaceRoleAlreadyAssignedError: new SpaceRoleAlreadyAssignedError(),
    LastOwnerError: new LastOwnerError(),
    PersonalSpaceImmutableError: new PersonalSpaceImmutableError(),
    NoAccountForEmailError: new NoAccountForEmailError(),
    AlreadyMemberError: new AlreadyMemberError(),
    InvitationAlreadyPendingError: new InvitationAlreadyPendingError(),
    InvitationNotFoundError: new InvitationNotFoundError(),
    InvitationNotPendingError: new InvitationNotPendingError(),
    InvitationExpiredError: new InvitationExpiredError(),
    RateLimitError: new RateLimitError(),
    NetworkError: new NetworkError(),
    ServerError: new ServerError(),
  }

  // Every namespace prefix the slice actually calls mapSpaceErrorToKey with.
  const PREFIXES = [
    'members', 'invitations', 'detail', 'accept',
    'create', 'edit', 'invite', 'leave', 'delete', 'transfer',
  ]

  async function makeI18n(language: 'en' | 'fr') {
    const instance = createInstance()
    await instance.init({
      lng: language,
      fallbackLng: false,
      ns: ['spaces'],
      defaultNS: 'spaces',
      resources: { en: { spaces: en }, fr: { spaces: fr } },
      interpolation: { escapeValue: false },
      initImmediate: false,
    })
    return instance
  }

  for (const language of ['en', 'fr'] as const) {
    it(`resolves every (error, prefix) pair to a real ${language} string, never the raw key`, async () => {
      const i18n = await makeI18n(language)
      for (const prefix of PREFIXES) {
        for (const [name, error] of Object.entries(ERRORS)) {
          const keys = mapSpaceErrorToKey(error, prefix)
          const resolved = i18n.t(keys)
          expect(resolved, `${name} under "${prefix}" (${language})`).not.toBe(keys[0])
          expect(resolved, `${name} under "${prefix}" (${language})`).not.toBe(keys[1])
          expect(typeof resolved).toBe('string')
          expect((resolved as string).length).toBeGreaterThan(0)
        }
      }
    })
  }
})
