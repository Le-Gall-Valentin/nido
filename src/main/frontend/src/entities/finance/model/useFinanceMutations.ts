import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useFinanceApi } from './financeApiContext'
import type { ContributionInput, RecurrenceInput, TransactionType } from './types'

// Every mutation invalidates the whole ['finance', spaceId] branch rather than tracking
// exact dependent keys one by one — transactions/budgets/stats/projection/balances are
// interdependent enough (any transaction change can shift stats, projection AND balances)
// that a narrower invalidation list would be easy to under-invalidate by accident.
function invalidateSpace(queryClient: ReturnType<typeof useQueryClient>, spaceId: string) {
  queryClient.invalidateQueries({ queryKey: ['finance', spaceId] })
}

export function useCreateCategory(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { label: string; color: string; icon: string }) => api.createCategory(spaceId, input.label, input.color, input.icon),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useUpdateCategory(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { categoryId: string; label: string; color: string; icon: string }) =>
      api.updateCategory(spaceId, input.categoryId, input.label, input.color, input.icon),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useDeleteCategory(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (categoryId: string) => api.deleteCategory(spaceId, categoryId),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useSetBudget(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { categoryId: string; monthlyLimit: number }) => api.setBudget(spaceId, input.categoryId, input.monthlyLimit),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useCreateTransaction(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: {
      label: string; amount: number; type: TransactionType; categoryId: string; date: string;
      payerId: string | null; contributors: ContributionInput[]
    }) => api.createTransaction(spaceId, input.label, input.amount, input.type, input.categoryId, input.date, input.payerId, input.contributors),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useUpdateTransaction(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: {
      transactionId: string; label: string; amount: number; type: TransactionType; categoryId: string; date: string;
      payerId: string | null; contributors: ContributionInput[]
    }) => api.updateTransaction(spaceId, input.transactionId, input.label, input.amount, input.type, input.categoryId,
      input.date, input.payerId, input.contributors),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useDeleteTransaction(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (transactionId: string) => api.deleteTransaction(spaceId, transactionId),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useMoveTransaction(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { transactionId: string; destinationSpaceId: string }) => api.moveTransaction(spaceId, input.transactionId, input.destinationSpaceId),
    onSuccess: (_data, variables) => {
      invalidateSpace(queryClient, spaceId)
      invalidateSpace(queryClient, variables.destinationSpaceId)
    },
  })
}

export function useCreateRecurringSeries(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: {
      label: string; amount: number; type: TransactionType; categoryId: string; payerId: string | null;
      contributors: ContributionInput[]; recurrence: RecurrenceInput
    }) => api.createRecurringSeries(spaceId, input.label, input.amount, input.type, input.categoryId, input.payerId, input.contributors, input.recurrence),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useUpdateRecurringSeries(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: {
      seriesId: string; label: string; amount: number; type: TransactionType; categoryId: string; payerId: string | null;
      contributors: ContributionInput[]; recurrence: RecurrenceInput
    }) => api.updateRecurringSeries(spaceId, input.seriesId, input.label, input.amount, input.type, input.categoryId,
      input.payerId, input.contributors, input.recurrence),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useDeleteRecurringSeries(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (seriesId: string) => api.deleteRecurringSeries(spaceId, seriesId),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useSettleDebt(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { fromMemberId: string; toMemberId: string; amount: number; date: string }) =>
      api.settleDebt(spaceId, input.fromMemberId, input.toMemberId, input.amount, input.date),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useCreateSavingsGoal(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { name: string; targetAmount: number; targetDate: string | null; color: string; glyph: string }) =>
      api.createSavingsGoal(spaceId, input.name, input.targetAmount, input.targetDate, input.color, input.glyph),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useUpdateSavingsGoal(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { goalId: string; name: string; targetAmount: number; targetDate: string | null; color: string; glyph: string }) =>
      api.updateSavingsGoal(spaceId, input.goalId, input.name, input.targetAmount, input.targetDate, input.color, input.glyph),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useDeleteSavingsGoal(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (goalId: string) => api.deleteSavingsGoal(spaceId, goalId),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}

export function useAddSavingsContribution(spaceId: string) {
  const api = useFinanceApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { goalId: string; memberId: string; amount: number; date: string }) =>
      api.addSavingsContribution(spaceId, input.goalId, input.memberId, input.amount, input.date),
    onSuccess: () => invalidateSpace(queryClient, spaceId),
  })
}
