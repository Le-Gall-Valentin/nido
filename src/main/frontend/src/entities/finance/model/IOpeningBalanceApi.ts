import type { OpeningBalance } from './types'

/** Port for the opening balance a space's balance is anchored on. */
export interface IOpeningBalanceApi {
  /** Null when the space has none. */
  getOpeningBalance(spaceId: string): Promise<OpeningBalance | null>
  setOpeningBalance(spaceId: string, amount: number, asOf: string): Promise<OpeningBalance>
  clearOpeningBalance(spaceId: string): Promise<void>
}
