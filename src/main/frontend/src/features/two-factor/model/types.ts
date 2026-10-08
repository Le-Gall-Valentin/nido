import type { TwoFactorMethod } from '@/entities/user'

/** Turning the authenticator app on: the secret to scan or type. */
export interface AppSetupData {
  otpauthUri: string
  secret: string
}

/** Turning the code by mail on: the code left for the account's address. */
export interface MailSetupData {
  sentTo: string
  resendAfterSeconds: number
}

/** A method as its holder sees it. Enabled and not usable: paused — the mail while mail is off. */
export interface MethodState {
  method: TwoFactorMethod
  enabled: boolean
  usable: boolean
}

/** A code left by mail; another can be asked for after this many seconds. */
export interface ResendData {
  resendAfterSeconds: number
}

/** What the choice screen moves on with: the app, or the mail with the wait before another code. */
export type CodeChoice = { method: 'APP' } | { method: 'MAIL'; resendAfterSeconds: number }
