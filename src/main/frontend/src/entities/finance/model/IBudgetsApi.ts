import type { Budget } from './types'

/** Port for reading and setting per-category monthly budgets. */
export interface IBudgetsApi {
  listBudgets(spaceId: string): Promise<Budget[]>
  setBudget(spaceId: string, categoryId: string, monthlyLimit: number): Promise<Budget>
}
