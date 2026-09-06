import { describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor, act } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import type { IFinanceApi } from './IFinanceApi'
import { FinanceApiProvider } from './financeApiContext'
import {
  useCreateCategory, useUpdateCategory, useDeleteCategory, useSetBudget, useDeleteBudget,
  useCreateTransaction, useUpdateTransaction, useDeleteTransaction,
  useCreateRecurringSeries, useUpdateRecurringSeries, useDeleteRecurringSeries,
  useSettleDebt, useCreateSavingsGoal, useUpdateSavingsGoal, useDeleteSavingsGoal, useAddSavingsContribution,
} from './useFinanceMutations'

function makeQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } })
}

function wrapper(api: IFinanceApi, queryClient: QueryClient) {
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

describe('useCreateCategory', () => {
  it('calls createCategory with every field', async () => {
    const api = fakeApi({ createCategory: vi.fn().mockResolvedValue({}) })
    const { result } = renderHook(() => useCreateCategory('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({ label: 'Loisirs', color: '#3b82f6', icon: 'Gamepad2' }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.createCategory).toHaveBeenCalledWith('space-1', 'Loisirs', '#3b82f6', 'Gamepad2')
  })
})

describe('useUpdateCategory', () => {
  it('calls updateCategory with every field', async () => {
    const api = fakeApi({ updateCategory: vi.fn().mockResolvedValue({}) })
    const { result } = renderHook(() => useUpdateCategory('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({ categoryId: 'c1', label: 'Loisirs', color: '#3b82f6', icon: 'Gamepad2' }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.updateCategory).toHaveBeenCalledWith('space-1', 'c1', 'Loisirs', '#3b82f6', 'Gamepad2')
  })
})

describe('useDeleteCategory', () => {
  it('calls deleteCategory with the category id', async () => {
    const api = fakeApi({ deleteCategory: vi.fn().mockResolvedValue(undefined) })
    const { result } = renderHook(() => useDeleteCategory('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate('c1'))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.deleteCategory).toHaveBeenCalledWith('space-1', 'c1')
  })
})

describe('useSetBudget', () => {
  it('calls setBudget with the category id and limit', async () => {
    const api = fakeApi({ setBudget: vi.fn().mockResolvedValue({}) })
    const { result } = renderHook(() => useSetBudget('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({ categoryId: 'c1', monthlyLimit: 150 }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.setBudget).toHaveBeenCalledWith('space-1', 'c1', 150)
  })
})

describe('useDeleteBudget', () => {
  it('calls deleteBudget with the category id', async () => {
    const api = fakeApi({ deleteBudget: vi.fn().mockResolvedValue(undefined) })
    const { result } = renderHook(() => useDeleteBudget('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate('c1'))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.deleteBudget).toHaveBeenCalledWith('space-1', 'c1')
  })
})

describe('useCreateTransaction', () => {
  it('calls createTransaction with every field', async () => {
    const created = { id: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [], recurring: false }
    const api = fakeApi({ createTransaction: vi.fn().mockResolvedValue(created) })
    const { result } = renderHook(() => useCreateTransaction('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({ label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [] }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.createTransaction).toHaveBeenCalledWith('space-1', 'Courses', 45.3, 'EXPENSE', 'c1', '2026-01-15', null, [])
  })

  it('invalidates every query under the space branch on success', async () => {
    const api = fakeApi({ createTransaction: vi.fn().mockResolvedValue({}) })
    const queryClient = makeQueryClient()
    queryClient.setQueryData(['finance', 'space-1', 'stats', '2026-01'], { totalExpense: 0 })
    queryClient.setQueryData(['finance', 'space-1', 'balances'], { netByMember: [] })
    queryClient.setQueryData(['finance', 'space-2', 'stats', '2026-01'], { totalExpense: 0 })
    const { result } = renderHook(() => useCreateTransaction('space-1'), { wrapper: wrapper(api, queryClient) })

    act(() => result.current.mutate({ label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [] }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(queryClient.getQueryState(['finance', 'space-1', 'stats', '2026-01'])?.isInvalidated).toBe(true)
    expect(queryClient.getQueryState(['finance', 'space-1', 'balances'])?.isInvalidated).toBe(true)
    expect(queryClient.getQueryState(['finance', 'space-2', 'stats', '2026-01'])?.isInvalidated).toBe(false)
  })
})

describe('useUpdateTransaction', () => {
  it('calls updateTransaction with every field', async () => {
    const api = fakeApi({ updateTransaction: vi.fn().mockResolvedValue({}) })
    const { result } = renderHook(() => useUpdateTransaction('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({
      transactionId: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [],
    }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.updateTransaction).toHaveBeenCalledWith('space-1', 't1', 'Courses', 45.3, 'EXPENSE', 'c1', '2026-01-15', null, [])
  })
})

describe('useDeleteTransaction', () => {
  it('calls deleteTransaction with the transaction id', async () => {
    const api = fakeApi({ deleteTransaction: vi.fn().mockResolvedValue(undefined) })
    const { result } = renderHook(() => useDeleteTransaction('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate('t1'))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.deleteTransaction).toHaveBeenCalledWith('space-1', 't1')
  })
})

describe('useCreateRecurringSeries', () => {
  it('calls createRecurringSeries with every field', async () => {
    const api = fakeApi({ createRecurringSeries: vi.fn().mockResolvedValue({}) })
    const { result } = renderHook(() => useCreateRecurringSeries('space-1'), { wrapper: wrapper(api, makeQueryClient()) })
    const recurrence = { intervalType: 'MONTHLY' as const, intervalCount: 1, anchorDate: '2026-01-01', endDate: null }

    act(() => result.current.mutate({ label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice', contributors: [], recurrence }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.createRecurringSeries).toHaveBeenCalledWith('space-1', 'Loyer', 800, 'EXPENSE', 'c1', 'alice', [], recurrence)
  })
})

describe('useUpdateRecurringSeries', () => {
  it('calls updateRecurringSeries with every field', async () => {
    const api = fakeApi({ updateRecurringSeries: vi.fn().mockResolvedValue({}) })
    const { result } = renderHook(() => useUpdateRecurringSeries('space-1'), { wrapper: wrapper(api, makeQueryClient()) })
    const recurrence = { intervalType: 'MONTHLY' as const, intervalCount: 1, anchorDate: '2026-01-01', endDate: null }

    act(() => result.current.mutate({ seriesId: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice', contributors: [], recurrence }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.updateRecurringSeries).toHaveBeenCalledWith('space-1', 's1', 'Loyer', 800, 'EXPENSE', 'c1', 'alice', [], recurrence)
  })
})

describe('useDeleteRecurringSeries', () => {
  it('calls deleteRecurringSeries with the series id', async () => {
    const api = fakeApi({ deleteRecurringSeries: vi.fn().mockResolvedValue(undefined) })
    const { result } = renderHook(() => useDeleteRecurringSeries('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate('s1'))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.deleteRecurringSeries).toHaveBeenCalledWith('space-1', 's1')
  })
})

describe('useSettleDebt', () => {
  it('calls settleDebt with every field', async () => {
    const api = fakeApi({ settleDebt: vi.fn().mockResolvedValue(undefined) })
    const { result } = renderHook(() => useSettleDebt('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({ fromMemberId: 'bob', toMemberId: 'alice', amount: 20, date: '2026-01-02' }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.settleDebt).toHaveBeenCalledWith('space-1', 'bob', 'alice', 20, '2026-01-02')
  })
})

describe('useCreateSavingsGoal', () => {
  it('calls createSavingsGoal with every field', async () => {
    const api = fakeApi({ createSavingsGoal: vi.fn().mockResolvedValue({}) })
    const { result } = renderHook(() => useCreateSavingsGoal('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({ name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯' }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.createSavingsGoal).toHaveBeenCalledWith('space-1', 'Vacances', 2000, null, '#5c7a58', '🎯')
  })
})

describe('useUpdateSavingsGoal', () => {
  it('calls updateSavingsGoal with every field', async () => {
    const api = fakeApi({ updateSavingsGoal: vi.fn().mockResolvedValue({}) })
    const { result } = renderHook(() => useUpdateSavingsGoal('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({ goalId: 'g1', name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯' }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.updateSavingsGoal).toHaveBeenCalledWith('space-1', 'g1', 'Vacances', 2000, null, '#5c7a58', '🎯')
  })
})

describe('useDeleteSavingsGoal', () => {
  it('calls deleteSavingsGoal with the goal id', async () => {
    const api = fakeApi({ deleteSavingsGoal: vi.fn().mockResolvedValue(undefined) })
    const { result } = renderHook(() => useDeleteSavingsGoal('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate('g1'))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.deleteSavingsGoal).toHaveBeenCalledWith('space-1', 'g1')
  })
})

describe('useAddSavingsContribution', () => {
  it('calls addSavingsContribution with every field', async () => {
    const api = fakeApi({ addSavingsContribution: vi.fn().mockResolvedValue(undefined) })
    const { result } = renderHook(() => useAddSavingsContribution('space-1'), { wrapper: wrapper(api, makeQueryClient()) })

    act(() => result.current.mutate({ goalId: 'g1', memberId: 'alice', amount: 100, date: '2026-01-05' }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.addSavingsContribution).toHaveBeenCalledWith('space-1', 'g1', 'alice', 100, '2026-01-05')
  })
})
