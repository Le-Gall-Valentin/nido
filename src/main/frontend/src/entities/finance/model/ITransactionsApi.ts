import type { ContributionInput, Transaction, TransactionType } from './types'

/** Port for one-off transaction CRUD. */
export interface ITransactionsApi {
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
}
