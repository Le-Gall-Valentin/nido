import type { Balances, SettlementRecord } from './types'

/** Port for who-owes-what balances between members and settling debts. */
export interface IBalancesApi {
  getBalances(spaceId: string): Promise<Balances>
  settleDebt(spaceId: string, fromMemberId: string, toMemberId: string, amount: number, date: string): Promise<void>
  listSettlements(spaceId: string, memberAId: string, memberBId: string): Promise<SettlementRecord[]>
}
