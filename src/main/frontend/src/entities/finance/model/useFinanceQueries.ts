import { useQuery } from '@tanstack/react-query'
import { useFinanceApi } from './financeApiContext'

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
export function savingsGoalsKey(spaceId: string) {
  return ['finance', spaceId, 'savings-goals'] as const
}

export function useCategories(spaceId: string | undefined) {
  const api = useFinanceApi()
  return useQuery({
    queryKey: categoriesKey(spaceId ?? ''),
    queryFn: () => api.listCategories(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useBudgets(spaceId: string | undefined) {
  const api = useFinanceApi()
  return useQuery({
    queryKey: budgetsKey(spaceId ?? ''),
    queryFn: () => api.listBudgets(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useTransactions(spaceId: string | undefined, month: string) {
  const api = useFinanceApi()
  return useQuery({
    queryKey: transactionsKey(spaceId ?? '', month),
    queryFn: () => api.listTransactions(spaceId as string, month),
    enabled: !!spaceId,
  })
}

export function useRecurringSeries(spaceId: string | undefined) {
  const api = useFinanceApi()
  return useQuery({
    queryKey: recurringSeriesKey(spaceId ?? ''),
    queryFn: () => api.listRecurringSeries(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useFinanceStats(spaceId: string | undefined, month: string) {
  const api = useFinanceApi()
  return useQuery({
    queryKey: financeStatsKey(spaceId ?? '', month),
    queryFn: () => api.getStats(spaceId as string, month),
    enabled: !!spaceId,
  })
}

export function useProjection(spaceId: string | undefined, month: string) {
  const api = useFinanceApi()
  return useQuery({
    queryKey: projectionKey(spaceId ?? '', month),
    queryFn: () => api.getProjection(spaceId as string, month),
    enabled: !!spaceId,
  })
}

export function useBalances(spaceId: string | undefined) {
  const api = useFinanceApi()
  return useQuery({
    queryKey: balancesKey(spaceId ?? ''),
    queryFn: () => api.getBalances(spaceId as string),
    enabled: !!spaceId,
  })
}

export function useSavingsGoals(spaceId: string | undefined) {
  const api = useFinanceApi()
  return useQuery({
    queryKey: savingsGoalsKey(spaceId ?? ''),
    queryFn: () => api.listSavingsGoals(spaceId as string),
    enabled: !!spaceId,
  })
}
