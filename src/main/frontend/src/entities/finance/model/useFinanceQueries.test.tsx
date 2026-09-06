import { describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import type { IFinanceApi } from './IFinanceApi'
import { FinanceApiProvider } from './financeApiContext'
import { useCategories, useTransactions, useFinanceStats, useBalances, useSettlementsBetween } from './useFinanceQueries'

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
    listBudgets: vi.fn(), setBudget: vi.fn(),
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
    const categories = [{ id: '1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }]
    const api = fakeApi({ listCategories: vi.fn().mockResolvedValue(categories) })

    const { result } = renderHook(() => useCategories('space-1'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(categories))
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

describe('useFinanceStats', () => {
  it('fetches stats for the given space and month', async () => {
    const stats = { balance: 100, totalExpense: 50, totalIncome: 150, remainingBudget: 350, breakdown: [], budgetVsActual: [] }
    const api = fakeApi({ getStats: vi.fn().mockResolvedValue(stats) })

    const { result } = renderHook(() => useFinanceStats('space-1', '2026-01'), { wrapper: wrapper(api) })

    await waitFor(() => expect(result.current.data).toEqual(stats))
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
