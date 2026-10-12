export type TransactionType = 'EXPENSE' | 'INCOME'
export type RecurrenceInterval = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export interface Category {
  id: string
  label: string
  color: string
  icon: string
  isDefault: boolean
  type: TransactionType
}

export interface Budget {
  id: string
  categoryId: string
  monthlyLimit: number
}

export interface Contribution {
  memberId: string
  shareAmount: number
}

/** A contributor as submitted to the API — null shareAmount requests an equal split. */
export interface ContributionInput {
  memberId: string
  shareAmount: number | null
}

export interface Transaction {
  id: string
  label: string
  amount: number
  type: TransactionType
  categoryId: string
  /** ISO date (YYYY-MM-DD). */
  date: string
  payerId: string | null
  contributors: Contribution[]
  recurring: boolean
}

export interface RecurrenceInput {
  intervalType: RecurrenceInterval
  intervalCount: number
  anchorDate: string
  endDate: string | null
}

export interface RecurringSeries {
  id: string
  label: string
  amount: number
  type: TransactionType
  categoryId: string
  payerId: string | null
  contributors: Contribution[]
  intervalType: RecurrenceInterval
  intervalCount: number
  anchorDate: string
  endDate: string | null
}

export interface CategoryAmount {
  categoryId: string
  amount: number
}

/** Decided by the server (`BudgetLine.status()`): WARNING from 80 % of the limit, OVER past it, and a 0 € budget is OVER from the first cent. */
export type BudgetStatus = 'OK' | 'WARNING' | 'OVER'

export interface BudgetLine {
  categoryId: string
  monthlyLimit: number
  spent: number
  status: BudgetStatus
}

export interface FinanceStats {
  /** Carried over from every month before: today's for the current month, the month end's for any other. */
  balance: number
  /** ISO date (YYYY-MM-DD) the balance is at. */
  balanceAsOf: string
  /** The month's own result: its income minus its expenses. */
  monthNet: number
  totalExpense: number
  totalIncome: number
  remainingBudget: number
  breakdown: CategoryAmount[]
  budgetVsActual: BudgetLine[]
}

export interface ProjectedOccurrence {
  seriesId: string
  label: string
  amount: number
  type: TransactionType
  date: string
}

export interface Projection {
  /** Everything recorded up to the month end, carried over from the months before. */
  recordedEndOfMonthBalance: number
  upcoming: ProjectedOccurrence[]
  projectedEndOfMonthBalance: number
}

/** The account's balance at the end of a day, as the bank shows it: every balance of the space is computed from it. */
export interface OpeningBalance {
  amount: number
  /** ISO date (YYYY-MM-DD). */
  asOf: string
}

export interface MemberBalance {
  memberId: string
  net: number
}

export interface SuggestedTransfer {
  fromMemberId: string
  toMemberId: string
  amount: number
}

export interface Balances {
  netByMember: MemberBalance[]
  suggestedTransfers: SuggestedTransfer[]
}

export interface SettlementRecord {
  id: string
  fromMemberId: string
  toMemberId: string
  amount: number
  date: string
}

export interface SavingsContribution {
  id: string
  memberId: string
  amount: number
  date: string
}

export interface SavingsGoal {
  id: string
  name: string
  targetAmount: number
  targetDate: string | null
  color: string
  glyph: string
  totalContributed: number
  contributions: SavingsContribution[]
}
