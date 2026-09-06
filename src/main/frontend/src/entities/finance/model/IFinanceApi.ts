import type {
  Balances, Budget, Category, ContributionInput, FinanceStats, Projection, RecurrenceInput,
  RecurringSeries, SavingsGoal, SettlementRecord, Transaction, TransactionType,
} from './types'

/**
 * Port for the Finance page. Consumers (hooks) depend on this contract,
 * never on the concrete axios-backed implementation, which is injected
 * through FinanceApiProvider.
 */
export interface IFinanceApi {
  listCategories(spaceId: string): Promise<Category[]>
  createCategory(spaceId: string, label: string, color: string, icon: string): Promise<Category>
  updateCategory(spaceId: string, categoryId: string, label: string, color: string, icon: string): Promise<Category>
  deleteCategory(spaceId: string, categoryId: string): Promise<void>

  listBudgets(spaceId: string): Promise<Budget[]>
  setBudget(spaceId: string, categoryId: string, monthlyLimit: number): Promise<Budget>

  listTransactions(spaceId: string, month: string): Promise<Transaction[]>
  createTransaction(
    spaceId: string, label: string, amount: number, type: TransactionType, categoryId: string, date: string,
    payerId: string | null, contributors: ContributionInput[]
  ): Promise<Transaction>
  updateTransaction(
    spaceId: string, transactionId: string, label: string, amount: number, type: TransactionType, categoryId: string,
    date: string, payerId: string | null, contributors: ContributionInput[]
  ): Promise<Transaction>
  deleteTransaction(spaceId: string, transactionId: string): Promise<void>

  listRecurringSeries(spaceId: string): Promise<RecurringSeries[]>
  createRecurringSeries(
    spaceId: string, label: string, amount: number, type: TransactionType, categoryId: string, payerId: string | null,
    contributors: ContributionInput[], recurrence: RecurrenceInput
  ): Promise<RecurringSeries>
  updateRecurringSeries(
    spaceId: string, seriesId: string, label: string, amount: number, type: TransactionType, categoryId: string,
    payerId: string | null, contributors: ContributionInput[], recurrence: RecurrenceInput
  ): Promise<RecurringSeries>
  deleteRecurringSeries(spaceId: string, seriesId: string): Promise<void>

  getStats(spaceId: string, month: string): Promise<FinanceStats>
  getProjection(spaceId: string, month: string): Promise<Projection>

  getBalances(spaceId: string): Promise<Balances>
  settleDebt(spaceId: string, fromMemberId: string, toMemberId: string, amount: number, date: string): Promise<void>
  listSettlements(spaceId: string, memberAId: string, memberBId: string): Promise<SettlementRecord[]>

  listSavingsGoals(spaceId: string): Promise<SavingsGoal[]>
  createSavingsGoal(spaceId: string, name: string, targetAmount: number, targetDate: string | null, color: string, glyph: string): Promise<SavingsGoal>
  updateSavingsGoal(spaceId: string, goalId: string, name: string, targetAmount: number, targetDate: string | null, color: string, glyph: string): Promise<SavingsGoal>
  deleteSavingsGoal(spaceId: string, goalId: string): Promise<void>
  addSavingsContribution(spaceId: string, goalId: string, memberId: string, amount: number, date: string): Promise<void>
}
