import type { FinanceStats, Projection } from './types'

/** Port for the monthly stats and end-of-month projection figures. */
export interface IFinanceStatsApi {
  getStats(spaceId: string, month: string): Promise<FinanceStats>
  getProjection(spaceId: string, month: string): Promise<Projection>
}
