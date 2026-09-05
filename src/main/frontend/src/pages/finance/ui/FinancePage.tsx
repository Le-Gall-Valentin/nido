import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Plus, Pencil, ArrowRightLeft, Trash2, Repeat, Settings2, Wallet, TrendingDown, TrendingUp, PiggyBank,
  AlertTriangle, HandCoins, Target,
} from 'lucide-react'
import { Alert, Spinner } from '@/shared/ui'
import { useAuth } from '@/features/auth'
import { useMySpaces, useWritableSpaces } from '@/features/space-switcher'
import { canWrite, isPersonal, useSpaceMembers, TransferDialog } from '@/entities/space'
import { UserAvatar } from '@/entities/user'
import {
  financeApi, FinanceApiProvider, useCategories, useTransactions, useFinanceStats, useProjection,
  useCreateTransaction, useCreateRecurringSeries, useUpdateTransaction, useDeleteTransaction, useMoveTransaction, useSetBudget,
  useCreateCategory, useUpdateCategory, useDeleteCategory, useBalances, useSettleDebt,
  useSavingsGoals, useCreateSavingsGoal, useUpdateSavingsGoal, useDeleteSavingsGoal, useAddSavingsContribution,
  type IFinanceApi, type Transaction, type SavingsGoal, type Category, type CategoryAmount,
} from '@/entities/finance'
import { CategoryIconBadge } from './CategoryIconBadge'
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

function StatCard({ icon: Icon, tintClassName, label, value }: { icon: typeof Wallet; tintClassName: string; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-border bg-bg-1 p-4">
      <div className={`grid size-9 shrink-0 place-items-center rounded-[10px] ${tintClassName}`}>
        <Icon size={18} />
      </div>
      <div className="min-w-0">
        <p className="truncate text-[13px] text-fg-3">{label}</p>
        <p className="text-[21px] font-semibold text-fg-0">{value}</p>
      </div>
    </div>
  )
}

/** A hand-drawn SVG donut (no charting library in this app) with a colored ring segment per category. */
function BreakdownDonut({ breakdown, categoryById }: { breakdown: CategoryAmount[]; categoryById: Map<string, Category> }) {
  const total = breakdown.reduce((sum, row) => sum + row.amount, 0)
  const radius = 52
  const circumference = 2 * Math.PI * radius
  let offset = 0
  return (
    <svg width={128} height={128} viewBox="0 0 128 128" className="-rotate-90 shrink-0">
      <circle cx={64} cy={64} r={radius} fill="none" stroke="var(--color-bg-3)" strokeWidth={20} />
      {total > 0 && breakdown.map((row) => {
        const category = categoryById.get(row.categoryId)
        const dash = (row.amount / total) * circumference
        const segmentOffset = offset
        offset += dash
        return (
          <circle key={row.categoryId} cx={64} cy={64} r={radius} fill="none"
            stroke={category?.color ?? 'var(--color-fg-3)'} strokeWidth={20}
            strokeDasharray={`${dash} ${circumference - dash}`} strokeDashoffset={-segmentOffset} />
        )
      })}
    </svg>
  )
}

function FinancePageContent() {
  const { t } = useTranslation('finance')
  const { spaceId = '' } = useParams<{ spaceId: string }>()
  const currentUserId = useAuth((s) => s.user)?.id ?? null
  const [month, setMonth] = useState(currentMonth())
  const { data: categories, isPending: categoriesPending, isError: categoriesError } = useCategories(spaceId)
  const { data: transactions, isPending, isError } = useTransactions(spaceId, month)
  const { data: stats } = useFinanceStats(spaceId, month)
  const { data: projection } = useProjection(spaceId, month)
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
  const breakdownTotal = (stats?.breakdown ?? []).reduce((sum, row) => sum + row.amount, 0)

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
    <div className="mx-auto max-w-[1100px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-fg-0">{t('title')}</h1>
        <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:flex-wrap sm:items-center">
          <input type="month" value={month} onChange={(e) => setMonth(e.target.value)}
            className="w-full rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-2.5 text-sm text-fg-0 outline-none focus:border-accent sm:w-auto" />
          <button type="button" onClick={() => setManagingCategories(true)}
            className="flex w-full items-center justify-center gap-1.5 rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-2.5 text-sm font-semibold text-fg-2 hover:bg-bg-2 sm:w-auto">
            <Settings2 size={16} /> {t('categories.manage')}
          </button>
          {canWriteHere && (
            <button type="button" onClick={() => setFormState({ mode: 'create' })}
              className="flex w-full items-center justify-center gap-1.5 rounded-[10px] bg-accent px-4 py-2.5 text-sm font-semibold text-white sm:w-auto">
              <Plus size={16} /> {t('new_transaction')}
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatCard icon={Wallet} tintClassName="bg-accent-dim text-accent" label={t('stats.balance')} value={formatAmount(stats?.balance ?? 0)} />
        <StatCard icon={TrendingDown} tintClassName="bg-status-red-dim text-status-red" label={t('stats.spent')} value={formatAmount(stats?.totalExpense ?? 0)} />
        <StatCard icon={TrendingUp} tintClassName="bg-status-green-dim text-status-green" label={t('stats.income')} value={formatAmount(stats?.totalIncome ?? 0)} />
        <StatCard icon={PiggyBank} tintClassName="bg-status-blue-dim text-status-blue" label={t('stats.remaining_budget')} value={formatAmount(stats?.remainingBudget ?? 0)} />
      </div>

      <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
        <h2 className="mb-3 text-[15px] font-semibold text-fg-0">{t('breakdown.title')}</h2>
        {(stats?.breakdown ?? []).length === 0 ? (
          <p className="text-sm text-fg-3">{t('breakdown.empty')}</p>
        ) : (
          <div className="flex flex-wrap items-center gap-6">
            <BreakdownDonut breakdown={stats?.breakdown ?? []} categoryById={categoryById} />
            <ul className="flex-1 space-y-2">
              {(stats?.breakdown ?? []).map((row) => {
                const category = categoryById.get(row.categoryId)
                const percent = breakdownTotal > 0 ? Math.round((row.amount / breakdownTotal) * 100) : 0
                return (
                  <li key={row.categoryId} className="flex items-center gap-2 text-sm">
                    <span className="size-2.5 shrink-0 rounded-[3px]" style={{ backgroundColor: category?.color }} />
                    <span className="flex-1 truncate text-fg-1">{category?.label ?? row.categoryId}</span>
                    <span className="text-fg-3">{percent}%</span>
                    <span className="w-20 text-right font-medium text-fg-0">{formatAmount(row.amount)}</span>
                  </li>
                )
              })}
            </ul>
          </div>
        )}
      </section>

      <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
        <h2 className="mb-3 text-[15px] font-semibold text-fg-0">{t('budget.title')}</h2>
        <ul className="space-y-4">
          {(stats?.budgetVsActual ?? []).map((line) => {
            const category = categoryById.get(line.categoryId)
            const ratio = line.monthlyLimit > 0 ? line.spent / line.monthlyLimit : 0
            const percent = Math.min(100, ratio * 100)
            const over = ratio > 1
            const warning = ratio >= 0.8 && !over
            const barColor = over ? 'var(--color-status-red)' : warning ? 'var(--color-status-orange)' : (category?.color ?? 'var(--color-accent)')
            return (
              <li key={line.categoryId}>
                <div className="flex items-center justify-between text-sm">
                  <span className="flex items-center gap-1.5 font-medium text-fg-1">
                    {(over || warning) && <AlertTriangle size={13} className={over ? 'text-status-red' : 'text-status-orange'} />}
                    {category?.label ?? line.categoryId}
                  </span>
                  {editingBudgetFor === line.categoryId ? (
                    <span className="flex items-center gap-1.5">
                      <input type="number" step="0.01" value={budgetInput} onChange={(e) => setBudgetInput(e.target.value)}
                        className="w-20 rounded-[8px] border-[1.5px] border-border bg-bg-1 px-2 py-1 text-sm text-fg-0 outline-none focus:border-accent" />
                      <button type="button" onClick={() => handleBudgetSave(line.categoryId)} className="text-sm font-semibold text-accent">{t('form.save')}</button>
                    </span>
                  ) : (
                    <span className="text-fg-2">
                      {formatAmount(line.spent)} / {formatAmount(line.monthlyLimit)}
                      {canWriteHere && (
                        <button type="button" onClick={() => { setEditingBudgetFor(line.categoryId); setBudgetInput(String(line.monthlyLimit)) }}
                          className="ml-2 text-xs font-semibold text-fg-3 hover:text-fg-1">{t('budget.edit')}</button>
                      )}
                    </span>
                  )}
                </div>
                <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-bg-2">
                  <div className="h-2 rounded-full transition-all" style={{ width: `${percent}%`, backgroundColor: barColor }} />
                </div>
                <p className={`mt-1 text-xs ${over ? 'text-status-red' : 'text-fg-3'}`}>
                  {over ? t('budget.over', { amount: formatAmount(line.spent - line.monthlyLimit) }) : t('budget.remaining', { amount: formatAmount(line.monthlyLimit - line.spent) })}
                </p>
              </li>
            )
          })}
        </ul>
      </section>

      <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
        <h2 className="mb-3 flex items-center gap-1.5 text-[15px] font-semibold text-fg-0">
          <Target size={16} className="text-fg-3" /> {t('projection.title')}
        </h2>
        <p className="text-sm text-fg-1">
          {t('projection.end_of_month_balance', { amount: formatAmount(projection?.projectedEndOfMonthBalance ?? 0) })}
        </p>
        {(projection?.upcoming.length ?? 0) > 0 && (
          <ul className="mt-2 space-y-1 text-sm text-fg-3">
            {projection?.upcoming.map((occurrence, i) => (
              <li key={i}>
                {occurrence.date} — {occurrence.label} ({occurrence.type === 'EXPENSE' ? '-' : '+'}{formatAmount(occurrence.amount)})
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
        <h2 className="mb-3 text-[15px] font-semibold text-fg-0">{t('transactions.title')}</h2>
        <ul className="divide-y divide-border">
          {(transactions ?? []).map((transaction) => {
            const category = categoryById.get(transaction.categoryId)
            const payer = members?.find((m) => m.userId === transaction.payerId)
            return (
              <li key={transaction.id} className="flex items-center gap-3 py-3">
                <CategoryIconBadge category={category} />
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-1.5 truncate text-sm font-medium text-fg-0">
                    {transaction.label}
                    {transaction.recurring && (
                      <span className="flex items-center gap-0.5 rounded-full bg-bg-2 px-1.5 py-0.5 text-[10.5px] font-semibold text-fg-3">
                        <Repeat size={10} /> {t('transactions.recurring')}
                      </span>
                    )}
                  </p>
                  <p className="truncate text-xs text-fg-3">{category?.label} · {transaction.date}</p>
                </div>
                {payer && <UserAvatar username={payer.username ?? '?'} role="USER" className="size-6 shrink-0 rounded-full text-[10px]" />}
                <span className={`shrink-0 text-sm font-semibold ${transaction.type === 'EXPENSE' ? 'text-fg-0' : 'text-status-green'}`}>
                  {transaction.type === 'EXPENSE' ? '-' : '+'}{formatAmount(transaction.amount)}
                </span>
                {canWriteHere && (
                  <span className="flex shrink-0 items-center gap-0.5">
                    <button type="button" aria-label={t('transactions.edit')} onClick={() => setFormState({ mode: 'edit', transaction })}
                      className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-2 hover:text-fg-1">
                      <Pencil size={15} />
                    </button>
                    <button type="button" aria-label={t('transactions.move')} onClick={() => setMovingTransaction(transaction)}
                      className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-2 hover:text-fg-1">
                      <ArrowRightLeft size={15} />
                    </button>
                    <button type="button" aria-label={t('transactions.delete')} onClick={() => setDeletingTransaction(transaction)}
                      className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-status-red-dim hover:text-status-red">
                      <Trash2 size={15} />
                    </button>
                  </span>
                )}
              </li>
            )
          })}
        </ul>
      </section>

      {!spaceIsPersonal && (
        <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
          <h2 className="mb-3 flex items-center gap-1.5 text-[15px] font-semibold text-fg-0">
            <HandCoins size={16} className="text-fg-3" /> {t('balances.title')}
          </h2>
          {(balances?.netByMember.length ?? 0) > 0 && (
            <ul className="mb-3 space-y-2">
              {balances?.netByMember.map((row) => (
                <li key={row.memberId} className="flex items-center gap-2.5 text-sm">
                  <UserAvatar username={memberLabel(row.memberId)} role="USER" className="size-7 rounded-full text-[11px]" />
                  <span className="flex-1 text-fg-1">{memberLabel(row.memberId)}</span>
                  <span className={`font-semibold ${row.net > 0 ? 'text-status-green' : row.net < 0 ? 'text-status-red' : 'text-fg-3'}`}>
                    {row.net > 0 ? '+' : ''}{formatAmount(row.net)}
                  </span>
                </li>
              ))}
            </ul>
          )}
          {(balances?.suggestedTransfers.length ?? 0) === 0 ? (
            <p className="text-sm text-fg-3">{t('balances.all_settled')}</p>
          ) : (
            <ul className="space-y-2">
              {balances?.suggestedTransfers.map((transfer, i) => (
                <li key={i} className="flex items-center justify-between rounded-[10px] bg-bg-2 px-3 py-2 text-sm">
                  <span className="text-fg-1">{memberLabel(transfer.fromMemberId)} → {memberLabel(transfer.toMemberId)}: <span className="font-semibold text-fg-0">{formatAmount(transfer.amount)}</span></span>
                  {canWriteHere && (
                    <button type="button" onClick={() => setSettlingTransfer(transfer)}
                      className="rounded-[8px] bg-accent px-2.5 py-1 text-xs font-semibold text-white">{t('balances.settle')}</button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {!spaceIsPersonal && (
        <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-[15px] font-semibold text-fg-0">{t('savings.title')}</h2>
            {canWriteHere && (
              <button type="button" onClick={() => setGoalFormState({ mode: 'create' })} className="text-sm font-semibold text-accent">{t('savings.new_goal')}</button>
            )}
          </div>
          <ul className="space-y-4">
            {(savingsGoals ?? []).map((goal) => {
              const percent = goal.targetAmount > 0 ? Math.min(100, (goal.totalContributed / goal.targetAmount) * 100) : 0
              return (
                <li key={goal.id}>
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium text-fg-1">{goal.name}</span>
                    <span className="text-fg-2">{t('savings.progress', { contributed: formatAmount(goal.totalContributed), target: formatAmount(goal.targetAmount) })}</span>
                  </div>
                  <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-bg-2">
                    <div className="h-2 rounded-full bg-accent transition-all" style={{ width: `${percent}%` }} />
                  </div>
                  {canWriteHere && (
                    <div className="mt-1.5 flex gap-3 text-xs font-semibold">
                      <button type="button" onClick={() => setContributingGoal(goal)} className="text-accent">{t('savings.contribute')}</button>
                      <button type="button" onClick={() => setGoalFormState({ mode: 'edit', goal })} className="text-fg-3 hover:text-fg-1">{t('savings.edit')}</button>
                      <button type="button" onClick={() => deleteSavingsGoal.mutate(goal.id)} className="text-fg-3 hover:text-status-red">{t('savings.delete')}</button>
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
          currentUserId={currentUserId}
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
