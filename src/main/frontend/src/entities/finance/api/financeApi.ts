import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { IFinanceApi } from '../model/IFinanceApi'
import type { Balances, Budget, Category, FinanceStats, Projection, RecurringSeries, SavingsGoal, SettlementRecord, Transaction } from '../model/types'

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    if (status === 404) throw new NotFoundError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const financeApi: IFinanceApi = {
  async listCategories(spaceId) {
    try {
      const res = await client.get<Category[]>(`/spaces/${spaceId}/finance/categories`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async createCategory(spaceId, label, color, icon, type) {
    try {
      const res = await client.post<Category>(`/spaces/${spaceId}/finance/categories`, { label, color, icon, type })
      return res.data
    } catch (error) { handleError(error) }
  },

  async updateCategory(spaceId, categoryId, label, color, icon) {
    try {
      const res = await client.patch<Category>(`/spaces/${spaceId}/finance/categories/${categoryId}`, { label, color, icon })
      return res.data
    } catch (error) { handleError(error) }
  },

  async deleteCategory(spaceId, categoryId) {
    try {
      await client.delete(`/spaces/${spaceId}/finance/categories/${categoryId}`)
    } catch (error) { handleError(error) }
  },

  async listBudgets(spaceId) {
    try {
      const res = await client.get<Budget[]>(`/spaces/${spaceId}/finance/budgets`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async setBudget(spaceId, categoryId, monthlyLimit) {
    try {
      const res = await client.put<Budget>(`/spaces/${spaceId}/finance/budgets/${categoryId}`, { monthlyLimit })
      return res.data
    } catch (error) { handleError(error) }
  },

  async deleteBudget(spaceId, categoryId) {
    try {
      await client.delete(`/spaces/${spaceId}/finance/budgets/${categoryId}`)
    } catch (error) { handleError(error) }
  },

  async listTransactions(spaceId, month) {
    try {
      const res = await client.get<Transaction[]>(`/spaces/${spaceId}/finance/transactions`, { params: { month } })
      return res.data
    } catch (error) { handleError(error) }
  },

  async createTransaction(spaceId, label, amount, type, categoryId, date, payerId, contributors) {
    try {
      const res = await client.post<Transaction>(`/spaces/${spaceId}/finance/transactions`,
        { label, amount, type, categoryId, date, payerId, contributors })
      return res.data
    } catch (error) { handleError(error) }
  },

  async updateTransaction(spaceId, transactionId, label, amount, type, categoryId, date, payerId, contributors) {
    try {
      const res = await client.patch<Transaction>(`/spaces/${spaceId}/finance/transactions/${transactionId}`,
        { label, amount, type, categoryId, date, payerId, contributors })
      return res.data
    } catch (error) { handleError(error) }
  },

  async deleteTransaction(spaceId, transactionId) {
    try {
      await client.delete(`/spaces/${spaceId}/finance/transactions/${transactionId}`)
    } catch (error) { handleError(error) }
  },

  async listRecurringSeries(spaceId) {
    try {
      const res = await client.get<RecurringSeries[]>(`/spaces/${spaceId}/finance/recurring-series`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async createRecurringSeries(spaceId, label, amount, type, categoryId, payerId, contributors, recurrence) {
    try {
      const res = await client.post<RecurringSeries>(`/spaces/${spaceId}/finance/recurring-series`,
        { label, amount, type, categoryId, payerId, contributors, recurrence })
      return res.data
    } catch (error) { handleError(error) }
  },

  async updateRecurringSeries(spaceId, seriesId, label, amount, type, categoryId, payerId, contributors, recurrence) {
    try {
      const res = await client.patch<RecurringSeries>(`/spaces/${spaceId}/finance/recurring-series/${seriesId}`,
        { label, amount, type, categoryId, payerId, contributors, recurrence })
      return res.data
    } catch (error) { handleError(error) }
  },

  async deleteRecurringSeries(spaceId, seriesId) {
    try {
      await client.delete(`/spaces/${spaceId}/finance/recurring-series/${seriesId}`)
    } catch (error) { handleError(error) }
  },

  async getStats(spaceId, month) {
    try {
      const res = await client.get<FinanceStats>(`/spaces/${spaceId}/finance/stats`, { params: { month } })
      return res.data
    } catch (error) { handleError(error) }
  },

  async getProjection(spaceId, month) {
    try {
      const res = await client.get<Projection>(`/spaces/${spaceId}/finance/projection`, { params: { month } })
      return res.data
    } catch (error) { handleError(error) }
  },

  async getBalances(spaceId) {
    try {
      const res = await client.get<Balances>(`/spaces/${spaceId}/finance/balances`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async settleDebt(spaceId, fromMemberId, toMemberId, amount, date) {
    try {
      await client.post(`/spaces/${spaceId}/finance/balances/settle`, { fromMemberId, toMemberId, amount, date })
    } catch (error) { handleError(error) }
  },

  async listSettlements(spaceId, memberAId, memberBId) {
    try {
      const res = await client.get<SettlementRecord[]>(`/spaces/${spaceId}/finance/balances/settlements`, {
        params: { memberAId, memberBId },
      })
      return res.data
    } catch (error) { handleError(error) }
  },

  async listSavingsGoals(spaceId) {
    try {
      const res = await client.get<SavingsGoal[]>(`/spaces/${spaceId}/finance/savings-goals`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async createSavingsGoal(spaceId, name, targetAmount, targetDate, color, glyph) {
    try {
      const res = await client.post<SavingsGoal>(`/spaces/${spaceId}/finance/savings-goals`, { name, targetAmount, targetDate, color, glyph })
      return res.data
    } catch (error) { handleError(error) }
  },

  async updateSavingsGoal(spaceId, goalId, name, targetAmount, targetDate, color, glyph) {
    try {
      const res = await client.patch<SavingsGoal>(`/spaces/${spaceId}/finance/savings-goals/${goalId}`, { name, targetAmount, targetDate, color, glyph })
      return res.data
    } catch (error) { handleError(error) }
  },

  async deleteSavingsGoal(spaceId, goalId) {
    try {
      await client.delete(`/spaces/${spaceId}/finance/savings-goals/${goalId}`)
    } catch (error) { handleError(error) }
  },

  async addSavingsContribution(spaceId, goalId, memberId, amount, date) {
    try {
      await client.post(`/spaces/${spaceId}/finance/savings-goals/${goalId}/contributions`, { memberId, amount, date })
    } catch (error) { handleError(error) }
  },
}
