import type { User } from '@/entities/user'

export interface LoginCredentials {
  /** What the person typed: their username or their email address. */
  identifier: string
  password: string
}

export type LoginApiResult =
  | { type: 'success'; user: User }
  /** `username` is the account's, given by the server — the person may have typed their address. */
  | { type: 'totp_required'; username: string }

export type LoginOutcome =
  | { kind: 'authenticated' }
  | { kind: 'totp_required'; username: string }
  | { kind: 'enrollment_proposed'; user: User }