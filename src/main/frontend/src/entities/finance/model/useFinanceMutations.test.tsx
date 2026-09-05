import { describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor, act } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import type { IFinanceApi } from './IFinanceApi'
import { FinanceApiProvider } from './financeApiContext'
import { useCreateTransaction, useSettleDebt } from './useFinanceMutations'

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
    listTransactions: vi.fn(), createTransaction: vi.fn(), updateTransaction: vi.fn(), deleteTransaction: vi.fn(), moveTransaction: vi.fn(),
    listRecurringSeries: vi.fn(), createRecurringSeries: vi.fn(), updateRecurringSeries: vi.fn(), deleteRecurringSeries: vi.fn(),
    getStats: vi.fn(), getProjection: vi.fn(),
    getBalances: vi.fn(), settleDebt: vi.fn(),
    listSavingsGoals: vi.fn(), createSavingsGoal: vi.fn(), updateSavingsGoal: vi.fn(), deleteSavingsGoal: vi.fn(), addSavingsContribution: vi.fn(),
    ...overrides,
  }
}

describe('useCreateTransaction', () => {
  it('calls createTransaction with every field', async () => {
    const created = { id: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [], recurring: false }
    const api = fakeApi({ createTransaction: vi.fn().mockResolvedValue(created) })
    const { result } = renderHook(() => useCreateTransaction('space-1'), { wrapper: wrapper(api) })

    act(() => result.current.mutate({ label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [] }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.createTransaction).toHaveBeenCalledWith('space-1', 'Courses', 45.3, 'EXPENSE', 'c1', '2026-01-15', null, [])
  })
})

describe('useSettleDebt', () => {
  it('calls settleDebt with every field', async () => {
    const api = fakeApi({ settleDebt: vi.fn().mockResolvedValue(undefined) })
    const { result } = renderHook(() => useSettleDebt('space-1'), { wrapper: wrapper(api) })

    act(() => result.current.mutate({ fromMemberId: 'bob', toMemberId: 'alice', amount: 20, date: '2026-01-02' }))

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(api.settleDebt).toHaveBeenCalledWith('space-1', 'bob', 'alice', 20, '2026-01-02')
  })
})
