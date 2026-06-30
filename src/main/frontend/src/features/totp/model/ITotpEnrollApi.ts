import type { TotpSetupData } from './types'

export interface ITotpEnrollApi {
  setup(): Promise<TotpSetupData>
  confirm(code: string): Promise<void>
  getStatus(): Promise<{ totpEnabled: boolean }>
  disable(code: string): Promise<void>
}