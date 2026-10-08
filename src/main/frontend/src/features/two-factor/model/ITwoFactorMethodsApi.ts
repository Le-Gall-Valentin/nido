import type { TwoFactorMethod } from '@/entities/user'
import type { AppSetupData, MailSetupData, MethodState, ResendData } from './types'

/** The signed-in person's own methods. */
export interface ITwoFactorMethodsApi {
  list(): Promise<MethodState[]>
  setupApp(): Promise<AppSetupData>
  /** Sends a code to the account's address — call it from a click, never from an effect. */
  setupMail(): Promise<MailSetupData>
  confirm(method: TwoFactorMethod, code: string): Promise<void>
  /** The code that turns the mail off — from a click. */
  sendDisableCode(): Promise<ResendData>
  /** `code` may be left out only for a paused method. */
  disable(method: TwoFactorMethod, code?: string): Promise<void>
}
