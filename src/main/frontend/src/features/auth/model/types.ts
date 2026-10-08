import type { TwoFactorMethod, User } from '@/entities/user'

export interface LoginCredentials {
  /** What the person typed: their username or their email address. */
  identifier: string
  password: string
}

/** A sign-in waiting for its second factor, as the server described it. */
export interface TwoFactorChallenge {
  /** The account's name — the person may have typed their address. */
  username: string
  /** Usable now, the app before the mail; two means the person chooses. */
  methods: TwoFactorMethod[]
  /** When the mail is among the methods. */
  maskedEmail: string | null
  /** When the mail was the only method: whether the server could send the code, and the wait. */
  mailCode: { sent: true; resendAfterSeconds: number } | { sent: false; retryAfterSeconds: number } | null
}

export type LoginApiResult =
  | { type: 'success'; user: User }
  | { type: 'two_factor_required'; challenge: TwoFactorChallenge }

export type LoginOutcome =
  | { kind: 'authenticated' }
  | { kind: 'two_factor_required'; challenge: TwoFactorChallenge }
  | { kind: 'enrollment_proposed'; user: User }
