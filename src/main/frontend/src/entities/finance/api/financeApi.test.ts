import { describe, expect, it, vi, beforeEach } from 'vitest'
import { AxiosError } from 'axios'
import { client } from '@/shared/api'
import { ForbiddenError, NotFoundError } from '@/shared/lib'
import { financeApi } from './financeApi'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), put: vi.fn(), delete: vi.fn() } }))

describe('financeApi', () => {
  beforeEach(() => vi.resetAllMocks())

  it('listCategories fetches the categories for a space', async () => {
    const categories = [{ id: '1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true, type: 'EXPENSE' }]
    vi.mocked(client.get).mockResolvedValueOnce({ data: categories })

    const result = await financeApi.listCategories('space-1')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/categories')
    expect(result).toEqual(categories)
  })

  it('createCategory posts label, color, icon and type', async () => {
    const category = { id: '1', label: 'Loisirs', color: '#3b82f6', icon: 'Gamepad2', isDefault: false, type: 'EXPENSE' }
    vi.mocked(client.post).mockResolvedValueOnce({ data: category })

    const result = await financeApi.createCategory('space-1', 'Loisirs', '#3b82f6', 'Gamepad2', 'EXPENSE')

    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/finance/categories', { label: 'Loisirs', color: '#3b82f6', icon: 'Gamepad2', type: 'EXPENSE' })
    expect(result).toEqual(category)
  })

  it('updateCategory patches the category with its label, color and icon', async () => {
    const category = { id: '1', label: 'Loisirs', color: '#3b82f6', icon: 'Gamepad2', isDefault: false, type: 'EXPENSE' }
    vi.mocked(client.patch).mockResolvedValueOnce({ data: category })

    const result = await financeApi.updateCategory('space-1', '1', 'Loisirs', '#3b82f6', 'Gamepad2')

    expect(client.patch).toHaveBeenCalledWith('/spaces/space-1/finance/categories/1', { label: 'Loisirs', color: '#3b82f6', icon: 'Gamepad2' })
    expect(result).toEqual(category)
  })

  it('deleteCategory deletes the category', async () => {
    vi.mocked(client.delete).mockResolvedValueOnce({ data: undefined })

    await financeApi.deleteCategory('space-1', '1')

    expect(client.delete).toHaveBeenCalledWith('/spaces/space-1/finance/categories/1')
  })

  it('listBudgets fetches the budgets for a space', async () => {
    const budgets = [{ categoryId: 'c1', monthlyLimit: 300 }]
    vi.mocked(client.get).mockResolvedValueOnce({ data: budgets })

    const result = await financeApi.listBudgets('space-1')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/budgets')
    expect(result).toEqual(budgets)
  })

  it('setBudget puts the monthly limit for a category', async () => {
    const budget = { categoryId: 'c1', monthlyLimit: 300 }
    vi.mocked(client.put).mockResolvedValueOnce({ data: budget })

    const result = await financeApi.setBudget('space-1', 'c1', 300)

    expect(client.put).toHaveBeenCalledWith('/spaces/space-1/finance/budgets/c1', { monthlyLimit: 300 })
    expect(result).toEqual(budget)
  })

  it('deleteBudget deletes the budget for a category', async () => {
    vi.mocked(client.delete).mockResolvedValueOnce({ data: undefined })

    await financeApi.deleteBudget('space-1', 'c1')

    expect(client.delete).toHaveBeenCalledWith('/spaces/space-1/finance/budgets/c1')
  })

  it('listTransactions fetches transactions for the requested month', async () => {
    const transactions = [{ id: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15', payerId: null, contributors: [], recurring: false }]
    vi.mocked(client.get).mockResolvedValueOnce({ data: transactions })

    const result = await financeApi.listTransactions('space-1', '2026-01')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/transactions', { params: { month: '2026-01' } })
    expect(result).toEqual(transactions)
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

  it('updateTransaction patches the transaction with every field', async () => {
    const transaction = { id: 't1', label: 'Courses', amount: 50, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-16', payerId: null, contributors: [], recurring: false }
    vi.mocked(client.patch).mockResolvedValueOnce({ data: transaction })

    const result = await financeApi.updateTransaction('space-1', 't1', 'Courses', 50, 'EXPENSE', 'c1', '2026-01-16', null, [])

    expect(client.patch).toHaveBeenCalledWith('/spaces/space-1/finance/transactions/t1', {
      label: 'Courses', amount: 50, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-16', payerId: null, contributors: [],
    })
    expect(result).toEqual(transaction)
  })

  it('deleteTransaction deletes the transaction', async () => {
    vi.mocked(client.delete).mockResolvedValueOnce({ data: undefined })

    await financeApi.deleteTransaction('space-1', 't1')

    expect(client.delete).toHaveBeenCalledWith('/spaces/space-1/finance/transactions/t1')
  })

  it('listRecurringSeries fetches the recurring series for a space', async () => {
    const series = [{
      id: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice',
      contributors: [], intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null,
    }]
    vi.mocked(client.get).mockResolvedValueOnce({ data: series })

    const result = await financeApi.listRecurringSeries('space-1')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/recurring-series')
    expect(result).toEqual(series)
  })

  it('createRecurringSeries posts every field including the recurrence', async () => {
    const recurrence = { intervalType: 'MONTHLY' as const, intervalCount: 1, anchorDate: '2026-01-01', endDate: null }
    const series = {
      id: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice',
      contributors: [], intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null,
    }
    vi.mocked(client.post).mockResolvedValueOnce({ data: series })

    const result = await financeApi.createRecurringSeries('space-1', 'Loyer', 800, 'EXPENSE', 'c1', 'alice', [], recurrence)

    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/finance/recurring-series', {
      label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice', contributors: [], recurrence,
    })
    expect(result).toEqual(series)
  })

  it('updateRecurringSeries patches every field including the recurrence', async () => {
    const recurrence = { intervalType: 'MONTHLY' as const, intervalCount: 1, anchorDate: '2026-01-01', endDate: null }
    const series = {
      id: 's1', label: 'Loyer révisé', amount: 850, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice',
      contributors: [], intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null,
    }
    vi.mocked(client.patch).mockResolvedValueOnce({ data: series })

    const result = await financeApi.updateRecurringSeries('space-1', 's1', 'Loyer révisé', 850, 'EXPENSE', 'c1', 'alice', [], recurrence)

    expect(client.patch).toHaveBeenCalledWith('/spaces/space-1/finance/recurring-series/s1', {
      label: 'Loyer révisé', amount: 850, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice', contributors: [], recurrence,
    })
    expect(result).toEqual(series)
  })

  it('deleteRecurringSeries deletes the series', async () => {
    vi.mocked(client.delete).mockResolvedValueOnce({ data: undefined })

    await financeApi.deleteRecurringSeries('space-1', 's1')

    expect(client.delete).toHaveBeenCalledWith('/spaces/space-1/finance/recurring-series/s1')
  })

  it('getStats fetches stats for the requested month', async () => {
    const stats = { balance: 100, totalExpense: 50, totalIncome: 150, remainingBudget: 350, breakdown: [], budgetVsActual: [] }
    vi.mocked(client.get).mockResolvedValueOnce({ data: stats })

    const result = await financeApi.getStats('space-1', '2026-01')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/stats', { params: { month: '2026-01' } })
    expect(result).toEqual(stats)
  })

  it('getProjection fetches the projection for the requested month', async () => {
    const projection = { actualBalanceSoFar: -50, upcoming: [], projectedEndOfMonthBalance: -850 }
    vi.mocked(client.get).mockResolvedValueOnce({ data: projection })

    const result = await financeApi.getProjection('space-1', '2026-01')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/projection', { params: { month: '2026-01' } })
    expect(result).toEqual(projection)
  })

  it('getBalances fetches the balances for a space', async () => {
    const balances = { netByMember: [], suggestedTransfers: [] }
    vi.mocked(client.get).mockResolvedValueOnce({ data: balances })

    const result = await financeApi.getBalances('space-1')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/balances')
    expect(result).toEqual(balances)
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

  it('listSavingsGoals fetches the savings goals for a space', async () => {
    const goals = [{ id: 'g1', name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯', totalContributed: 0, contributions: [] }]
    vi.mocked(client.get).mockResolvedValueOnce({ data: goals })

    const result = await financeApi.listSavingsGoals('space-1')

    expect(client.get).toHaveBeenCalledWith('/spaces/space-1/finance/savings-goals')
    expect(result).toEqual(goals)
  })

  it('createSavingsGoal posts name, target amount, target date, color and glyph', async () => {
    const goal = { id: 'g1', name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯', totalContributed: 0, contributions: [] }
    vi.mocked(client.post).mockResolvedValueOnce({ data: goal })

    const result = await financeApi.createSavingsGoal('space-1', 'Vacances', 2000, null, '#5c7a58', '🎯')

    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/finance/savings-goals', { name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯' })
    expect(result).toEqual(goal)
  })

  it('updateSavingsGoal patches name, target amount, target date, color and glyph', async () => {
    const goal = { id: 'g1', name: 'Vacances', targetAmount: 2500, targetDate: null, color: '#5c7a58', glyph: '🎯', totalContributed: 0, contributions: [] }
    vi.mocked(client.patch).mockResolvedValueOnce({ data: goal })

    const result = await financeApi.updateSavingsGoal('space-1', 'g1', 'Vacances', 2500, null, '#5c7a58', '🎯')

    expect(client.patch).toHaveBeenCalledWith('/spaces/space-1/finance/savings-goals/g1', { name: 'Vacances', targetAmount: 2500, targetDate: null, color: '#5c7a58', glyph: '🎯' })
    expect(result).toEqual(goal)
  })

  it('deleteSavingsGoal deletes the goal', async () => {
    vi.mocked(client.delete).mockResolvedValueOnce({ data: undefined })

    await financeApi.deleteSavingsGoal('space-1', 'g1')

    expect(client.delete).toHaveBeenCalledWith('/spaces/space-1/finance/savings-goals/g1')
  })

  it('addSavingsContribution posts member, amount and date', async () => {
    vi.mocked(client.post).mockResolvedValueOnce({ data: undefined })

    await financeApi.addSavingsContribution('space-1', 'g1', 'alice', 100, '2026-01-05')

    expect(client.post).toHaveBeenCalledWith('/spaces/space-1/finance/savings-goals/g1/contributions', { memberId: 'alice', amount: 100, date: '2026-01-05' })
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
