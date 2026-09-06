import type { SavingsGoal } from './types'

/** Port for savings goals and contributing to them. */
export interface ISavingsGoalsApi {
  listSavingsGoals(spaceId: string): Promise<SavingsGoal[]>
  createSavingsGoal(spaceId: string, name: string, targetAmount: number, targetDate: string | null, color: string, glyph: string): Promise<SavingsGoal>
  updateSavingsGoal(spaceId: string, goalId: string, name: string, targetAmount: number, targetDate: string | null, color: string, glyph: string): Promise<SavingsGoal>
  deleteSavingsGoal(spaceId: string, goalId: string): Promise<void>
  addSavingsContribution(spaceId: string, goalId: string, memberId: string, amount: number, date: string): Promise<void>
}
