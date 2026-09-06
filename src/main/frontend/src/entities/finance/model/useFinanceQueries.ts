import { useQuery } from '@tanstack/react-query'
import { useFinanceApi } from './financeApiContext'
import type { ICategoriesApi } from './ICategoriesApi'
import type { IBudgetsApi } from './IBudgetsApi'
import type { ITransactionsApi } from './ITransactionsApi'
import type { IRecurringSeriesApi } from './IRecurringSeriesApi'
import type { IFinanceStatsApi } from './IFinanceStatsApi'
import type { IBalancesApi } from './IBalancesApi'
import type { ISavingsGoalsApi } from './ISavingsGoalsApi'

export function categoriesKey(spaceId: string) {
  return ['finance', spaceId, 'categories'] as const
}
export function budgetsKey(spaceId: string) {
  return ['finance', spaceId, 'budgets'] as const
}
export function transactionsKey(spaceId: string, month: string) {
  return ['finance', spaceId, 'transactions', month] as const
}
export function recurringSeriesKey(spaceId: string) {
  return ['finance', spaceId, 'recurring-series'] as const
}
export function financeStatsKey(spaceId: string, month: string) {
  return ['finance', spaceId, 'stats', month] as const
}
export function projectionKey(spaceId: string, month: string) {
  return ['finance', spaceId, 'projection', month] as const
}
export function balancesKey(spaceId: string) {
  return ['finance', spaceId, 'balances'] as const
}
export function settlementsBetweenKey(spaceId: string, memberAId: string, memberBId: string) {
  return ['finance', spaceId, 'settlements', memberAId, memberBId] as const
}
export function savingsGoalsKey(spaceId: string) {
  return ['finance', spaceId, 'savings-goals'] as const
}

export function useCategories(spaceId: string | undefined) {
  const api: ICategoriesApi = useFinanceApi()
  return useQuery({
    queryKey: categoriesKey(spaceId ?? ''),
    queryFn: () => api.listCategories(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useBudgets(spaceId: string | undefined) {
  const api: IBudgetsApi = useFinanceApi()
  return useQuery({
    queryKey: budgetsKey(spaceId ?? ''),
    queryFn: () => api.listBudgets(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useTransactions(spaceId: string | undefined, month: string) {
  const api: ITransactionsApi = useFinanceApi()
  return useQuery({
    queryKey: transactionsKey(spaceId ?? '', month),
    queryFn: () => api.listTransactions(spaceId as string, month),
    enabled: !!spaceId,
  })
}

export function useRecurringSeries(spaceId: string | undefined) {
  const api: IRecurringSeriesApi = useFinanceApi()
  return useQuery({
    queryKey: recurringSeriesKey(spaceId ?? ''),
    queryFn: () => api.listRecurringSeries(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useFinanceStats(spaceId: string | undefined, month: string) {
  const api: IFinanceStatsApi = useFinanceApi()
  return useQuery({
    queryKey: financeStatsKey(spaceId ?? '', month),
    queryFn: () => api.getStats(spaceId as string, month),
    enabled: !!spaceId,
  })
}

export function useProjection(spaceId: string | undefined, month: string) {
  const api: IFinanceStatsApi = useFinanceApi()
  return useQuery({
    queryKey: projectionKey(spaceId ?? '', month),
    queryFn: () => api.getProjection(spaceId as string, month),
    enabled: !!spaceId,
  })
}

export function useBalances(spaceId: string | undefined) {
  const api: IBalancesApi = useFinanceApi()
  return useQuery({
    queryKey: balancesKey(spaceId ?? ''),
    queryFn: () => api.getBalances(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useSettlementsBetween(spaceId: string | undefined, memberAId: string | undefined, memberBId: string | undefined) {
  const api: IBalancesApi = useFinanceApi()
  return useQuery({
    queryKey: settlementsBetweenKey(spaceId ?? '', memberAId ?? '', memberBId ?? ''),
    queryFn: () => api.listSettlements(spaceId as string, memberAId as string, memberBId as string),
    enabled: !!spaceId && !!memberAId && !!memberBId,
  })
}

export function useSavingsGoals(spaceId: string | undefined) {
  const api: ISavingsGoalsApi = useFinanceApi()
  return useQuery({
    queryKey: savingsGoalsKey(spaceId ?? ''),
    queryFn: () => api.listSavingsGoals(spaceId as string),
    enabled: !!spaceId,
  })
}
