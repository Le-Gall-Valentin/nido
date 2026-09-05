export type TransactionType = 'EXPENSE' | 'INCOME'
export type RecurrenceInterval = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export interface Category {
  id: string
  label: string
  color: string
  icon: string
  isDefault: boolean
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

export interface BudgetLine {
  categoryId: string
  monthlyLimit: number
  spent: number
}

export interface FinanceStats {
  balance: number
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
  actualBalanceSoFar: number
  upcoming: ProjectedOccurrence[]
  projectedEndOfMonthBalance: number
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
