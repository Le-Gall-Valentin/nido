import { describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import type { IFinanceApi } from './IFinanceApi'
import { FinanceApiProvider } from './financeApiContext'
import {
  useCategories, useBudgets, useTransactions, useRecurringSeries, useFinanceStats, useProjection,
  useBalances, useSettlementsBetween, useSavingsGoals,
} from './useFinanceQueries'

function wrapper(api: IFinanceApi) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <FinanceApiProvider api={api}>{children}</FinanceApiProvider>
    </QueryClientProvider>
  )
}

function fakeApi(overrides: Partial<IFinanceApi> = {}): IFinanceApi {
  return {
    listCategories: vi.fn(), createCategory: vi.fn(), updateCategory: vi.fn(), deleteCategory: vi.fn(),
    listBudgets: vi.fn(), setBudget: vi.fn(), deleteBudget: vi.fn(),
    listTransactions: vi.fn(), createTransaction: vi.fn(), updateTransaction: vi.fn(), deleteTransaction: vi.fn(),
    listRecurringSeries: vi.fn(), createRecurringSeries: vi.fn(), updateRecurringSeries: vi.fn(), deleteRecurringSeries: vi.fn(),
    getStats: vi.fn(), getProjection: vi.fn(),
    getBalances: vi.fn(), settleDebt: vi.fn(), listSettlements: vi.fn(),
    listSavingsGoals: vi.fn(), createSavingsGoal: vi.fn(), updateSavingsGoal: vi.fn(), deleteSavingsGoal: vi.fn(), addSavingsContribution: vi.fn(),
    ...overrides,
  }
}

describe('useCategories', () => {
  it('fetches categories for the given space', async () => {
    const categories = [{ id: '1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true, type: 'EXPENSE' }]
    const api = fakeApi({ listCategories: vi.fn().mockResolvedValue(categories) })

    const { result } = renderHook(() => useCategories('space-1'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(categories))
  })
})

describe('useBudgets', () => {
  it('fetches budgets for the given space', async () => {
    const budgets = [{ categoryId: 'c1', monthlyLimit: 300 }]
    const api = fakeApi({ listBudgets: vi.fn().mockResolvedValue(budgets) })

    const { result } = renderHook(() => useBudgets('space-1'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(budgets))
    expect(api.listBudgets).toHaveBeenCalledWith('space-1')
  })
})

describe('useTransactions', () => {
  it('fetches transactions for the given space and month', async () => {
    const transactions = [{ id: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [], recurring: false }]
    const api = fakeApi({ listTransactions: vi.fn().mockResolvedValue(transactions) })

    const { result } = renderHook(() => useTransactions('space-1', '2026-01'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(transactions))
    expect(api.listTransactions).toHaveBeenCalledWith('space-1', '2026-01')
  })
})

describe('useRecurringSeries', () => {
  it('fetches recurring series for the given space', async () => {
    const series = [{
      id: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice',
      contributors: [], intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null,
    }]
    const api = fakeApi({ listRecurringSeries: vi.fn().mockResolvedValue(series) })

    const { result } = renderHook(() => useRecurringSeries('space-1'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(series))
    expect(api.listRecurringSeries).toHaveBeenCalledWith('space-1')
  })
})

describe('useFinanceStats', () => {
  it('fetches stats for the given space and month', async () => {
    const stats = { balance: 100, totalExpense: 50, totalIncome: 150, remainingBudget: 350, breakdown: [], budgetVsActual: [] }
    const api = fakeApi({ getStats: vi.fn().mockResolvedValue(stats) })

    const { result } = renderHook(() => useFinanceStats('space-1', '2026-01'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(stats))
  })
})

describe('useProjection', () => {
  it('fetches the projection for the given space and month', async () => {
    const projection = { actualBalanceSoFar: -50, upcoming: [], projectedEndOfMonthBalance: -850 }
    const api = fakeApi({ getProjection: vi.fn().mockResolvedValue(projection) })

    const { result } = renderHook(() => useProjection('space-1', '2026-01'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(projection))
    expect(api.getProjection).toHaveBeenCalledWith('space-1', '2026-01')
  })
})

describe('useBalances', () => {
  it('fetches balances for the given space', async () => {
    const balances = { netByMember: [], suggestedTransfers: [] }
    const api = fakeApi({ getBalances: vi.fn().mockResolvedValue(balances) })

    const { result } = renderHook(() => useBalances('space-1'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(balances))
  })
})

describe('useSettlementsBetween', () => {
  it('fetches settlements between the two given members', async () => {
    const settlements = [{ id: 's1', fromMemberId: 'u-1', toMemberId: 'u-2', amount: 20, date: '2026-01-02' }]
    const api = fakeApi({ listSettlements: vi.fn().mockResolvedValue(settlements) })

    const { result } = renderHook(() => useSettlementsBetween('space-1', 'u-1', 'u-2'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(settlements))
    expect(api.listSettlements).toHaveBeenCalledWith('space-1', 'u-1', 'u-2')
  })
})

describe('useSavingsGoals', () => {
  it('fetches savings goals for the given space', async () => {
    const goals = [{ id: 'g1', name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯', totalContributed: 0, contributions: [] }]
    const api = fakeApi({ listSavingsGoals: vi.fn().mockResolvedValue(goals) })

    const { result } = renderHook(() => useSavingsGoals('space-1'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(goals))
    expect(api.listSavingsGoals).toHaveBeenCalledWith('space-1')
  })
})
