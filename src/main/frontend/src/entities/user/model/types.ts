import type { Language } from '@/shared/lib'

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER'

/** A way to prove a sign-in after the password, as the server names it: the authenticator app, or a code sent by mail. */
export type TwoFactorMethod = 'APP' | 'MAIL'

export interface User {
  id: string
  username: string
  email: string
  role: UserRole
  createdAt: string
  /** The methods on, in order — paused ones included (the mail while mail is off). */
  twoFactorMethods: TwoFactorMethod[]
  /** The language recorded on the account; null until a signed-in session records one. */
  language?: Language | null
}

/** The invitation of an account that has not chosen its password yet: its link still works, or expired. */
export interface InvitationState {
  status: 'pending' | 'expired'
  expiresAt: string
}

/** User as seen by admin endpoints — includes account state. */
export interface AdminUser extends User {
  isActive: boolean
  /** Null once the account chose its password. */
  invitation: InvitationState | null
}

/** How an invitation left: by mail, or as a link for the administrator to pass on — shown this once. */
export type InvitationDelivery = { delivery: 'mail' } | { delivery: 'link'; link: string }

export function isAdminRole(role?: UserRole): boolean {
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
}