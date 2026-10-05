/** Router state the welcome page leaves on the login page's history entry once the first password is chosen. */
export function invitationAcceptedState(identifier: string) {
  return { invitation: 'accepted', identifier } as const
}

/** The identifier to fill in after an accepted invitation; null when the login page was reached otherwise. */
export function acceptedInvitationIdentifier(state: unknown): string | null {
  const found = state as { invitation?: unknown; identifier?: unknown } | null
  return found?.invitation === 'accepted' && typeof found.identifier === 'string' ? found.identifier : null
}
