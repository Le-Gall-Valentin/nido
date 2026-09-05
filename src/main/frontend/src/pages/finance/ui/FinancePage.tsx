import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Plus, Pencil, ArrowRightLeft, Trash2, Repeat, Settings2 } from 'lucide-react'
import { Alert, Dialog, Spinner } from '@/shared/ui'
import { useMySpaces, useWritableSpaces } from '@/features/space-switcher'
import { canWrite, isPersonal, useSpaceMembers, TransferDialog } from '@/entities/space'
import {
  financeApi, FinanceApiProvider, useCategories, useBudgets, useTransactions, useFinanceStats,
  useCreateTransaction, useCreateRecurringSeries, useUpdateTransaction, useDeleteTransaction, useMoveTransaction, useSetBudget,
  useCreateCategory, useUpdateCategory, useDeleteCategory, useBalances, useSettleDebt,
  useSavingsGoals, useCreateSavingsGoal, useUpdateSavingsGoal, useDeleteSavingsGoal, useAddSavingsContribution,
  type IFinanceApi, type Transaction, type SavingsGoal,
} from '@/entities/finance'
import { resolveCategoryIcon } from '../lib/resolveCategoryIcon'
import { TransactionFormModal, type TransactionFormInput } from './TransactionFormModal'
import { DeleteTransactionModal } from './DeleteTransactionModal'
import { CategoryManagerModal } from './CategoryManagerModal'
import { SettleDebtModal } from './SettleDebtModal'
import { SavingsGoalFormModal, type SavingsGoalFormInput } from './SavingsGoalFormModal'
import { AddContributionModal } from './AddContributionModal'

interface FinancePageProps {
  api?: IFinanceApi
}

export function FinancePage({ api = financeApi }: FinancePageProps = {}) {
  return (
    <FinanceApiProvider api={api}>
      <FinancePageContent />
    </FinanceApiProvider>
  )
}

function currentMonth(): string {
  return new Date().toISOString().slice(0, 7)
}

function formatAmount(amount: number): string {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(amount)
}

function FinancePageContent() {
  const { t } = useTranslation('finance')
  const { spaceId = '' } = useParams<{ spaceId: string }>()
  const [month, setMonth] = useState(currentMonth())
  const { data: categories, isPending: categoriesPending, isError: categoriesError } = useCategories(spaceId)
  const { data: budgets } = useBudgets(spaceId)
  const { data: transactions, isPending, isError } = useTransactions(spaceId, month)
  const { data: stats } = useFinanceStats(spaceId, month)
  const { data: members } = useSpaceMembers(spaceId)
  const { data: mySpaces } = useMySpaces()
  const { data: writableDestinations } = useWritableSpaces(spaceId)

  const createTransaction = useCreateTransaction(spaceId)
  const createRecurringSeries = useCreateRecurringSeries(spaceId)
  const updateTransaction = useUpdateTransaction(spaceId)
  const deleteTransaction = useDeleteTransaction(spaceId)
  const moveTransaction = useMoveTransaction(spaceId)
  const setBudget = useSetBudget(spaceId)
  const createCategory = useCreateCategory(spaceId)
  const updateCategory = useUpdateCategory(spaceId)
  const deleteCategory = useDeleteCategory(spaceId)
  const { data: balances } = useBalances(spaceId)
  const settleDebt = useSettleDebt(spaceId)
  const { data: savingsGoals } = useSavingsGoals(spaceId)
  const createSavingsGoal = useCreateSavingsGoal(spaceId)
  const updateSavingsGoal = useUpdateSavingsGoal(spaceId)
  const deleteSavingsGoal = useDeleteSavingsGoal(spaceId)
  const addSavingsContribution = useAddSavingsContribution(spaceId)

  const [formState, setFormState] = useState<{ mode: 'create' } | { mode: 'edit'; transaction: Transaction } | null>(null)
  const [deletingTransaction, setDeletingTransaction] = useState<Transaction | null>(null)
  const [movingTransaction, setMovingTransaction] = useState<Transaction | null>(null)
  const [editingBudgetFor, setEditingBudgetFor] = useState<string | null>(null)
  const [budgetInput, setBudgetInput] = useState('')
  const [managingCategories, setManagingCategories] = useState(false)
  const [categoryDeleteError, setCategoryDeleteError] = useState<string | null>(null)
  const [settlingTransfer, setSettlingTransfer] = useState<{ fromMemberId: string; toMemberId: string; amount: number } | null>(null)
  const [goalFormState, setGoalFormState] = useState<{ mode: 'create' } | { mode: 'edit'; goal: SavingsGoal } | null>(null)
  const [contributingGoal, setContributingGoal] = useState<SavingsGoal | null>(null)

  function handleDeleteCategory(categoryId: string) {
    setCategoryDeleteError(null)
    deleteCategory.mutate(categoryId, { onError: () => setCategoryDeleteError('in_use') })
  }

  function memberLabel(memberId: string): string {
    return members?.find((m) => m.userId === memberId)?.username ?? memberId
  }

  function handleGoalFormSubmit(input: SavingsGoalFormInput) {
    if (goalFormState?.mode === 'edit') {
      updateSavingsGoal.mutate(
        { goalId: goalFormState.goal.id, ...input },
        { onSuccess: () => setGoalFormState(null) }
      )
      return
    }
    createSavingsGoal.mutate(input, { onSuccess: () => setGoalFormState(null) })
  }

  const currentSpace = mySpaces?.find((s) => s.id === spaceId)
  const canWriteHere = currentSpace ? canWrite(currentSpace.myRole) : false
  const spaceIsPersonal = currentSpace ? isPersonal(currentSpace) : false

  const categoryById = new Map((categories ?? []).map((c) => [c.id, c]))

  function handleFormSubmit(input: TransactionFormInput) {
    if (formState?.mode === 'edit') {
      updateTransaction.mutate(
        { transactionId: formState.transaction.id, label: input.label, amount: input.amount, type: input.type,
          categoryId: input.categoryId, date: input.date, payerId: input.payerId, contributors: input.contributors },
        { onSuccess: () => setFormState(null) }
      )
      return
    }
    if (input.recurrence) {
      createRecurringSeries.mutate(
        { label: input.label, amount: input.amount, type: input.type, categoryId: input.categoryId, payerId: input.payerId,
          contributors: input.contributors, recurrence: input.recurrence },
        { onSuccess: () => setFormState(null) }
      )
      return
    }
    createTransaction.mutate(
      { label: input.label, amount: input.amount, type: input.type, categoryId: input.categoryId, date: input.date,
        payerId: input.payerId, contributors: input.contributors },
      { onSuccess: () => setFormState(null) }
    )
  }

  async function handleMoveConfirm(destinationSpaceId: string): Promise<void> {
    if (!movingTransaction) return
    await moveTransaction.mutateAsync({ transactionId: movingTransaction.id, destinationSpaceId })
  }

  function handleBudgetSave(categoryId: string) {
    const value = Number(budgetInput)
    if (!Number.isNaN(value) && value >= 0) {
      setBudget.mutate({ categoryId, monthlyLimit: value }, { onSuccess: () => setEditingBudgetFor(null) })
    }
  }

  if (isPending || categoriesPending) return <Spinner label={t('loading')} fullscreen={false} />
  if (isError || categoriesError) return <Alert variant="error">{t('error.load_failed')}</Alert>

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">{t('title')}</h1>
        <div className="flex items-center gap-2">
          <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} className="rounded-md border px-3 py-2 text-sm" />
          <button type="button" onClick={() => setManagingCategories(true)} className="flex items-center gap-1 rounded-md border px-3 py-2 text-sm">
            <Settings2 size={16} /> {t('categories.manage')}
          </button>
          {canWriteHere && (
            <button type="button" onClick={() => setFormState({ mode: 'create' })}
              className="flex items-center gap-1 rounded-md bg-fg-1 px-3 py-2 text-sm text-bg-1">
              <Plus size={16} /> {t('new_transaction')}
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <div className="rounded-lg border p-4"><p className="text-sm text-fg-3">{t('stats.balance')}</p><p className="text-lg font-semibold">{formatAmount(stats?.balance ?? 0)}</p></div>
        <div className="rounded-lg border p-4"><p className="text-sm text-fg-3">{t('stats.spent')}</p><p className="text-lg font-semibold">{formatAmount(stats?.totalExpense ?? 0)}</p></div>
        <div className="rounded-lg border p-4"><p className="text-sm text-fg-3">{t('stats.income')}</p><p className="text-lg font-semibold">{formatAmount(stats?.totalIncome ?? 0)}</p></div>
        <div className="rounded-lg border p-4"><p className="text-sm text-fg-3">{t('stats.remaining_budget')}</p><p className="text-lg font-semibold">{formatAmount(stats?.remainingBudget ?? 0)}</p></div>
      </div>

      <section className="rounded-lg border p-4">
        <h2 className="mb-3 font-medium">{t('breakdown.title')}</h2>
        <ul className="space-y-2">
          {(stats?.breakdown ?? []).map((row) => {
            const category = categoryById.get(row.categoryId)
            return (
              <li key={row.categoryId} className="flex items-center justify-between text-sm">
                <span className="flex items-center gap-2">
                  <span className="h-2 w-2 rounded-full" style={{ backgroundColor: category?.color }} />
                  {category?.label ?? row.categoryId}
                </span>
                <span>{formatAmount(row.amount)}</span>
              </li>
            )
          })}
        </ul>
      </section>

      <section className="rounded-lg border p-4">
        <h2 className="mb-3 font-medium">{t('budget.title')}</h2>
        <ul className="space-y-3">
          {(stats?.budgetVsActual ?? []).map((line) => {
            const category = categoryById.get(line.categoryId)
            const percent = line.monthlyLimit > 0 ? Math.min(100, (line.spent / line.monthlyLimit) * 100) : 0
            const over = line.spent > line.monthlyLimit
            return (
              <li key={line.categoryId}>
                <div className="flex items-center justify-between text-sm">
                  <span>{category?.label ?? line.categoryId}</span>
                  {editingBudgetFor === line.categoryId ? (
                    <span className="flex items-center gap-1">
                      <input type="number" step="0.01" value={budgetInput} onChange={(e) => setBudgetInput(e.target.value)}
                        className="w-20 rounded-md border px-2 py-1 text-sm" />
                      <button type="button" onClick={() => handleBudgetSave(line.categoryId)} className="text-sm text-fg-1 underline">{t('form.save')}</button>
                    </span>
                  ) : (
                    <span>
                      {formatAmount(line.spent)} / {formatAmount(line.monthlyLimit)}
                      {canWriteHere && (
                        <button type="button" onClick={() => { setEditingBudgetFor(line.categoryId); setBudgetInput(String(line.monthlyLimit)) }}
                          className="ml-2 text-xs text-fg-3 underline">{t('budget.edit')}</button>
                      )}
                    </span>
                  )}
                </div>
                <div className="mt-1 h-2 rounded-full bg-bg-2">
                  <div className={`h-2 rounded-full ${over ? 'bg-red-500' : 'bg-fg-1'}`} style={{ width: `${percent}%` }} />
                </div>
                <p className="mt-1 text-xs text-fg-3">
                  {over ? t('budget.over', { amount: formatAmount(line.spent - line.monthlyLimit) }) : t('budget.remaining', { amount: formatAmount(line.monthlyLimit - line.spent) })}
                </p>
              </li>
            )
          })}
        </ul>
      </section>

      <section className="rounded-lg border p-4">
        <h2 className="mb-3 font-medium">{t('transactions.title')}</h2>
        <ul className="divide-y">
          {(transactions ?? []).map((transaction) => {
            const category = categoryById.get(transaction.categoryId)
            const Icon = resolveCategoryIcon(category?.icon ?? 'Circle')
            return (
              <li key={transaction.id} className="flex items-center justify-between py-3">
                <span className="flex items-center gap-3">
                  <Icon size={18} color={category?.color} />
                  <span>
                    <span className="block text-sm font-medium">{transaction.label}{transaction.recurring && <Repeat size={12} className="ml-1 inline" />}</span>
                    <span className="block text-xs text-fg-3">{category?.label} · {transaction.date}</span>
                  </span>
                </span>
                <span className="flex items-center gap-3">
                  <span className={transaction.type === 'EXPENSE' ? 'text-red-600' : 'text-green-600'}>
                    {transaction.type === 'EXPENSE' ? '-' : '+'}{formatAmount(transaction.amount)}
                  </span>
                  {canWriteHere && (
                    <>
                      <button type="button" aria-label={t('transactions.edit')} onClick={() => setFormState({ mode: 'edit', transaction })}><Pencil size={16} /></button>
                      <button type="button" aria-label={t('transactions.move')} onClick={() => setMovingTransaction(transaction)}><ArrowRightLeft size={16} /></button>
                      <button type="button" aria-label={t('transactions.delete')} onClick={() => setDeletingTransaction(transaction)}><Trash2 size={16} /></button>
                    </>
                  )}
                </span>
              </li>
            )
          })}
        </ul>
      </section>

      {!spaceIsPersonal && (
        <section className="rounded-lg border p-4">
          <h2 className="mb-3 font-medium">{t('balances.title')}</h2>
          {(balances?.suggestedTransfers.length ?? 0) === 0 ? (
            <p className="text-sm text-fg-3">{t('balances.all_settled')}</p>
          ) : (
            <ul className="space-y-2">
              {balances?.suggestedTransfers.map((transfer, i) => (
                <li key={i} className="flex items-center justify-between text-sm">
                  <span>{memberLabel(transfer.fromMemberId)} → {memberLabel(transfer.toMemberId)}: {formatAmount(transfer.amount)}</span>
                  {canWriteHere && (
                    <button type="button" onClick={() => setSettlingTransfer(transfer)} className="text-sm text-fg-1 underline">{t('balances.settle')}</button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {!spaceIsPersonal && (
        <section className="rounded-lg border p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-medium">{t('savings.title')}</h2>
            {canWriteHere && (
              <button type="button" onClick={() => setGoalFormState({ mode: 'create' })} className="text-sm text-fg-1 underline">{t('savings.new_goal')}</button>
            )}
          </div>
          <ul className="space-y-3">
            {(savingsGoals ?? []).map((goal) => {
              const percent = goal.targetAmount > 0 ? Math.min(100, (goal.totalContributed / goal.targetAmount) * 100) : 0
              return (
                <li key={goal.id}>
                  <div className="flex items-center justify-between text-sm">
                    <span>{goal.name}</span>
                    <span>{t('savings.progress', { contributed: formatAmount(goal.totalContributed), target: formatAmount(goal.targetAmount) })}</span>
                  </div>
                  <div className="mt-1 h-2 rounded-full bg-bg-2">
                    <div className="h-2 rounded-full bg-fg-1" style={{ width: `${percent}%` }} />
                  </div>
                  {canWriteHere && (
                    <div className="mt-1 flex gap-3 text-xs">
                      <button type="button" onClick={() => setContributingGoal(goal)} className="underline">{t('savings.contribute')}</button>
                      <button type="button" onClick={() => setGoalFormState({ mode: 'edit', goal })} className="underline">{t('savings.edit')}</button>
                      <button type="button" onClick={() => deleteSavingsGoal.mutate(goal.id)} className="underline">{t('savings.delete')}</button>
                    </div>
                  )}
                </li>
              )
            })}
          </ul>
        </section>
      )}

      {formState && (
        <TransactionFormModal
          mode={formState.mode}
          transaction={formState.mode === 'edit' ? formState.transaction : undefined}
          categories={categories ?? []}
          members={members ?? []}
          canPickContributors={!spaceIsPersonal}
          onSubmit={handleFormSubmit}
          onCancel={() => setFormState(null)}
        />
      )}

      {deletingTransaction && (
        <DeleteTransactionModal
          label={deletingTransaction.label}
          isPending={deleteTransaction.isPending}
          error={deleteTransaction.isError ? 'error' : null}
          onCancel={() => setDeletingTransaction(null)}
          onConfirm={() => deleteTransaction.mutate(deletingTransaction.id, { onSuccess: () => setDeletingTransaction(null) })}
        />
      )}

      {movingTransaction && (
        <TransferDialog
          itemName={movingTransaction.label}
          operation="move"
          destinations={writableDestinations ?? []}
          onClose={() => setMovingTransaction(null)}
          onConfirm={handleMoveConfirm}
        />
      )}

      {managingCategories && (
        <CategoryManagerModal
          categories={categories ?? []}
          onCreate={(label, color, icon) => createCategory.mutate({ label, color, icon })}
          onUpdate={(categoryId, label, color, icon) => updateCategory.mutate({ categoryId, label, color, icon })}
          onDelete={handleDeleteCategory}
          onClose={() => setManagingCategories(false)}
          deleteError={categoryDeleteError}
        />
      )}

      {settlingTransfer && (
        <SettleDebtModal
          fromLabel={memberLabel(settlingTransfer.fromMemberId)}
          toLabel={memberLabel(settlingTransfer.toMemberId)}
          amount={settlingTransfer.amount}
          isPending={settleDebt.isPending}
          onCancel={() => setSettlingTransfer(null)}
          onConfirm={(date) => settleDebt.mutate(
            { fromMemberId: settlingTransfer.fromMemberId, toMemberId: settlingTransfer.toMemberId, amount: settlingTransfer.amount, date },
            { onSuccess: () => setSettlingTransfer(null) }
          )}
        />
      )}

      {goalFormState && (
        <SavingsGoalFormModal
          mode={goalFormState.mode}
          goal={goalFormState.mode === 'edit' ? goalFormState.goal : undefined}
          onSubmit={handleGoalFormSubmit}
          onCancel={() => setGoalFormState(null)}
        />
      )}

      {contributingGoal && (
        <AddContributionModal
          goalName={contributingGoal.name}
          members={members ?? []}
          isPending={addSavingsContribution.isPending}
          onCancel={() => setContributingGoal(null)}
          onSubmit={(input) => addSavingsContribution.mutate(
            { goalId: contributingGoal.id, ...input },
            { onSuccess: () => setContributingGoal(null) }
          )}
        />
      )}
    </div>
  )
}
