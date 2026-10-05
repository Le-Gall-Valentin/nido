import { describe, expect, it } from 'vitest'
import { acceptedInvitationIdentifier, invitationAcceptedState } from './invitationAccepted'

describe('invitationAccepted', () => {
  it('carries the identifier to fill in from the welcome page to the login page', () => {
    expect(acceptedInvitationIdentifier(invitationAcceptedState('carol'))).toBe('carol')
  })

  it('finds nothing in any other history state', () => {
    expect(acceptedInvitationIdentifier(null)).toBeNull()
    expect(acceptedInvitationIdentifier({ passwordReset: 'done' })).toBeNull()
    expect(acceptedInvitationIdentifier({ invitation: 'accepted', identifier: 42 })).toBeNull()
  })
})
