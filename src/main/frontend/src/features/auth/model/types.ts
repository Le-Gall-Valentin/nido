import type { User } from '@/entities/user'

export interface LoginCredentials {
  username: string
  password: string
}

export type LoginApiResult =
  | { type: 'success'; user: User }
  | { type: 'totp_required' }

export type LoginOutcome =
  | { kind: 'authenticated' }
  | { kind: 'totp_required'; username: string }
  | { kind: 'enrollment_proposed'; user: User }