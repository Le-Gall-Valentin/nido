import { useAuth } from '@/features/auth'
import type { LinkCheckState } from '@/shared/lib'

export type LinkStep = 'checking' | 'signed_in' | 'form' | 'invalid' | 'unavailable'

/**
 * What a one-time link's page shows. Someone signed in on this device who follows the link signs out first —
 * setting a password for another account, or their own from inside a session, would confuse more than it
 * helps — and the token stays with the page meanwhile. While a session is being restored, nothing is decided.
 */
export function useLinkStep(linkState: LinkCheckState, refusedOnSave: boolean) {
  const user = useAuth((s) => s.user)
  const isRestoringSession = useAuth((s) => s.isInitializing)
  const logout = useAuth((s) => s.logout)
  const linkStep: LinkStep = refusedOnSave ? 'invalid' : linkState === 'valid' ? 'form' : linkState
  const step: LinkStep = isRestoringSession ? 'checking' : user ? 'signed_in' : linkStep
  // The store clears the session in its own finally block: a failing call still signs this device out, and the
  // catch only keeps the rejection from surfacing unhandled.
  const signOut = () => { logout().catch(() => {}) }
  return { step, user, signOut }
}
