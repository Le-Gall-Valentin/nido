import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AxiosError } from 'axios'
import { client } from '@/shared/api'
import { ForbiddenError, NotFoundError } from '@/shared/lib'
import { financeApi } from './financeApi'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), put: vi.fn(), delete: vi.fn() } }))

describe('financeApi', () => {
  beforeEach(() => vi.resetAllMocks())

  it('listCategories fetches the categories for a space', async () => {
    const categories = [{ id: '1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }]
    vi.mocked(client.get).mockResolvedValueOnce({ data: categories })

    const result = await financeApi.listCategories('space-1')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/categories')
    expect(result).toEqual(categories)
  })

  it('createTransaction posts label, amount, type, category, date, payer and contributors', async () => {
    const transaction = { id: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [], recurring: false }
    vi.mocked(client.post).mockResolvedValueOnce({ data: transaction })

    const result = await financeApi.createTransaction('space-1', 'Courses', 45.3, 'EXPENSE', 'c1', '2026-01-15', null, [])

    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/finance/transactions', {
      label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [],
    })
    expect(result).toEqual(transaction)
  })

  it('getStats fetches stats for the requested month', async () => {
    const stats = { balance: 100, totalExpense: 50, totalIncome: 150, remainingBudget: 350, breakdown: [], budgetVsActual: [] }
    vi.mocked(client.get).mockResolvedValueOnce({ data: stats })

    const result = await financeApi.getStats('space-1', '2026-01')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/stats', { params: { month: '2026-01' } })
    expect(result).toEqual(stats)
  })

  it('settleDebt posts the settlement fields', async () => {
    vi.mocked(client.post).mockResolvedValueOnce({ data: {} })

    await financeApi.settleDebt('space-1', 'bob', 'alice', 20, '2026-01-02')

    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/finance/balances/settle', {
      fromMemberId: 'bob', toMemberId: 'alice', amount: 20, date: '2026-01-02',
    })
  })

  it('listSettlements fetches settlements between the two given members', async () => {
    const settlements = [{ id: 's1', fromMemberId: 'bob', toMemberId: 'alice', amount: 20, date: '2026-01-02' }]
    vi.mocked(client.get).mockResolvedValueOnce({ data: settlements })

    const result = await financeApi.listSettlements('space-1', 'bob', 'alice')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/balances/settlements', {
      params: { memberAId: 'bob', memberBId: 'alice' },
    })
    expect(result).toEqual(settlements)
  })

  it('translates a 403 response into ForbiddenError', async () => {
    vi.mocked(client.get).mockRejectedValueOnce(new AxiosError('Forbidden', undefined, undefined, undefined, { status: 403 } as never))

    await expect(financeApi.listCategories('space-1')).rejects.toBeInstanceOf(ForbiddenError)
  })

  it('translates a 404 response into NotFoundError', async () => {
    vi.mocked(client.delete).mockRejectedValueOnce(new AxiosError('Not found', undefined, undefined, undefined, { status: 404 } as never))

    await expect(financeApi.deleteTransaction('space-1', 't1')).rejects.toBeInstanceOf(NotFoundError)
  })
})
