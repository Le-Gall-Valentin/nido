import { useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Plus, Pencil, ArrowRightLeft, ArrowRight, Trash2, Repeat, Settings2, Wallet, TrendingDown, TrendingUp, PiggyBank,
  AlertTriangle, HandCoins, Target, CheckCircle2,
} from 'lucide-react'
import { Alert, Spinner } from '@/shared/ui'
import { useAuth } from '@/features/auth'
import { useMySpaces, useWritableSpaces } from '@/features/space-switcher'
import { canWrite, isPersonal, useSpaceMembers, TransferDialog } from '@/entities/space'
import { UserAvatar } from '@/entities/user'
import {
  financeApi, FinanceApiProvider, useCategories, useTransactions, useFinanceStats, useProjection,
  useCreateTransaction, useCreateRecurringSeries, useUpdateTransaction, useDeleteTransaction, useMoveTransaction, useSetBudget,
  useCreateCategory, useUpdateCategory, useDeleteCategory, useBalances, useSettleDebt, useSettlementsBetween,
  useSavingsGoals, useCreateSavingsGoal, useUpdateSavingsGoal, useDeleteSavingsGoal, useAddSavingsContribution,
  useRecurringSeries, useUpdateRecurringSeries, useDeleteRecurringSeries,
  type IFinanceApi, type Transaction, type SavingsGoal, type Category, type CategoryAmount, type RecurringSeries,
} from '@/entities/finance'
import { CategoryIconBadge } from './CategoryIconBadge'
import { TransactionFormModal, type TransactionFormInput } from './TransactionFormModal'
import { DeleteTransactionModal } from './DeleteTransactionModal'
import { TransactionDetailModal } from './TransactionDetailModal'
import { CategoryTransactionsModal } from './CategoryTransactionsModal'
import { MemberTransactionsModal } from './MemberTransactionsModal'
import { SettlementHistoryModal } from './SettlementHistoryModal'
import { CategoryManagerModal } from './CategoryManagerModal'
import { BudgetManagerModal } from './BudgetManagerModal'
import { RecurringSeriesManagerModal } from './RecurringSeriesManagerModal'
import { RecurringSeriesFormModal, type RecurringSeriesFormInput } from './RecurringSeriesFormModal'
import { SettleDebtModal } from './SettleDebtModal'
import { SavingsGoalFormModal, type SavingsGoalFormInput } from './SavingsGoalFormModal'
import { AddContributionModal } from './AddContributionModal'
import { formatAmount } from '../lib/formatAmount'

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
  const containerRef = useRef<HTMLDivElement>(null)
  const [hovered, setHovered] = useState<{ categoryId: string; x: number; y: number } | null>(null)
  const total = breakdown.reduce((sum, row) => sum + row.amount, 0)
  const radius = 52
  const circumference = 2 * Math.PI * radius
  let offset = 0

  function handlePointerMove(categoryId: string, e: React.MouseEvent) {
    const rect = containerRef.current?.getBoundingClientRect()
    if (!rect) return
    setHovered({ categoryId, x: e.clientX - rect.left, y: e.clientY - rect.top })
  }

  const hoveredRow = hovered ? breakdown.find((row) => row.categoryId === hovered.categoryId) : null
  const hoveredCategory = hoveredRow ? categoryById.get(hoveredRow.categoryId) : null
  const hoveredPercent = hoveredRow && total > 0 ? Math.round((hoveredRow.amount / total) * 100) : 0

  return (
    <div ref={containerRef} className="relative shrink-0">
      <svg width={128} height={128} viewBox="0 0 128 128" className="-rotate-90">
        <circle cx={64} cy={64} r={radius} fill="none" stroke="var(--color-bg-3)" strokeWidth={20} />
        {total > 0 && breakdown.map((row) => {
          const category = categoryById.get(row.categoryId)
          const dash = (row.amount / total) * circumference
          const segmentOffset = offset
          offset += dash
          const percent = Math.round((row.amount / total) * 100)
          return (
            <circle key={row.categoryId} cx={64} cy={64} r={radius} fill="none"
              stroke={category?.color ?? 'var(--color-fg-3)'} strokeWidth={20}
              strokeDasharray={`${dash} ${circumference - dash}`} strokeDashoffset={-segmentOffset}
              role="img" aria-label={`${category?.label ?? row.categoryId}: ${percent}%, ${formatAmount(row.amount)}`}
              onMouseEnter={(e) => handlePointerMove(row.categoryId, e)}
              onMouseMove={(e) => handlePointerMove(row.categoryId, e)}
              onMouseLeave={() => setHovered(null)} />
          )
        })}
      </svg>
      {hovered && hoveredRow && (
        <div role="tooltip"
          className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-[calc(100%+10px)] whitespace-nowrap rounded-[8px] border border-border bg-bg-1 px-2.5 py-1.5 text-xs shadow-[0_4px_16px_rgba(44,42,38,0.16)]"
          style={{ left: hovered.x, top: hovered.y }}>
          <p className="flex items-center gap-1.5 font-medium text-fg-0">
            <span className="size-2 shrink-0 rounded-[2px]" style={{ backgroundColor: hoveredCategory?.color }} />
            {hoveredCategory?.label ?? hoveredRow.categoryId}
          </p>
          <p className="text-fg-3">{hoveredPercent}% · {formatAmount(hoveredRow.amount)}</p>
        </div>
      )}
    </div>
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
  const { data: recurringSeries } = useRecurringSeries(spaceId)
  const { data: members } = useSpaceMembers(spaceId)
  const { data: mySpaces } = useMySpaces()
  const { data: writableDestinations } = useWritableSpaces(spaceId)

  const createTransaction = useCreateTransaction(spaceId)
  const createRecurringSeries = useCreateRecurringSeries(spaceId)
  const updateRecurringSeries = useUpdateRecurringSeries(spaceId)
  const deleteRecurringSeries = useDeleteRecurringSeries(spaceId)
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
  const [viewingTransaction, setViewingTransaction] = useState<Transaction | null>(null)
  const [viewingCategoryId, setViewingCategoryId] = useState<string | null>(null)
  const [managingCategories, setManagingCategories] = useState(false)
  const [managingBudget, setManagingBudget] = useState(false)
  const [managingRecurringSeries, setManagingRecurringSeries] = useState(false)
  const [editingSeries, setEditingSeries] = useState<RecurringSeries | null>(null)
  const [deletingSeries, setDeletingSeries] = useState<RecurringSeries | null>(null)
  const [categoryDeleteError, setCategoryDeleteError] = useState<string | null>(null)
  const [settlingTransfer, setSettlingTransfer] = useState<{ fromMemberId: string; toMemberId: string; amount: number } | null>(null)
  const [viewingMemberId, setViewingMemberId] = useState<string | null>(null)
  const [viewingHistoryBetween, setViewingHistoryBetween] = useState<{ memberAId: string; memberBId: string } | null>(null)
  const [goalFormState, setGoalFormState] = useState<{ mode: 'create' } | { mode: 'edit'; goal: SavingsGoal } | null>(null)
  const [contributingGoal, setContributingGoal] = useState<SavingsGoal | null>(null)

  const { data: settlementsBetween } = useSettlementsBetween(spaceId, viewingHistoryBetween?.memberAId, viewingHistoryBetween?.memberBId)

  function handleDeleteCategory(categoryId: string) {
    setCategoryDeleteError(null)
    deleteCategory.mutate(categoryId, { onError: () => setCategoryDeleteError('in_use') })
  }

  function handleUpdateSeriesSubmit(input: RecurringSeriesFormInput) {
    if (!editingSeries) return
    updateRecurringSeries.mutate({ seriesId: editingSeries.id, ...input }, { onSuccess: () => setEditingSeries(null) })
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

  const paidByMember = new Map<string, number>()
  for (const transaction of transactions ?? []) {
    if (transaction.type === 'EXPENSE' && transaction.payerId) {
      paidByMember.set(transaction.payerId, (paidByMember.get(transaction.payerId) ?? 0) + transaction.amount)
    }
  }

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

      <div className="grid grid-cols-[repeat(auto-fit,minmax(min(210px,100%),1fr))] gap-3">
        <StatCard icon={Wallet} tintClassName="bg-accent-dim text-accent" label={t('stats.balance')} value={formatAmount(stats?.balance ?? 0)} />
        <StatCard icon={TrendingDown} tintClassName="bg-status-red-dim text-status-red" label={t('stats.spent')} value={formatAmount(stats?.totalExpense ?? 0)} />
        <StatCard icon={TrendingUp} tintClassName="bg-status-green-dim text-status-green" label={t('stats.income')} value={formatAmount(stats?.totalIncome ?? 0)} />
        <StatCard icon={PiggyBank} tintClassName="bg-status-blue-dim text-status-blue" label={t('stats.remaining_budget')} value={formatAmount(stats?.remainingBudget ?? 0)} />
      </div>

      <div className="mt-4 grid grid-cols-[repeat(auto-fit,minmax(min(360px,100%),1fr))] gap-4">
        <section className="flex flex-col rounded-2xl border border-border bg-bg-1 p-4">
          <h2 className="mb-3 text-[15px] font-semibold text-fg-0">{t('breakdown.title')}</h2>
          <div className="flex flex-1 items-center justify-center">
            {(stats?.breakdown ?? []).length === 0 ? (
              <p className="text-sm text-fg-3">{t('breakdown.empty')}</p>
            ) : (
              <div className="flex w-full flex-col items-center gap-4 sm:flex-row sm:gap-6">
                <BreakdownDonut breakdown={stats?.breakdown ?? []} categoryById={categoryById} />
                <ul className="w-full space-y-2 sm:w-auto sm:flex-1">
                  {(stats?.breakdown ?? []).map((row) => {
                    const category = categoryById.get(row.categoryId)
                    const percent = breakdownTotal > 0 ? Math.round((row.amount / breakdownTotal) * 100) : 0
                    return (
                      <li key={row.categoryId}>
                        <button type="button" onClick={() => setViewingCategoryId(row.categoryId)}
                          className="flex w-full items-center gap-2 rounded-lg text-left text-sm transition-colors hover:bg-bg-2">
                          <span className="size-2.5 shrink-0 rounded-[3px]" style={{ backgroundColor: category?.color }} />
                          <span className="flex-1 truncate text-fg-1">{category?.label ?? row.categoryId}</span>
                          <span className="text-fg-3">{percent}%</span>
                          <span className="w-20 text-right font-medium text-fg-0">{formatAmount(row.amount)}</span>
                        </button>
                      </li>
                    )
                  })}
                </ul>
              </div>
            )}
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-bg-1 p-4">
          <div className="mb-3 flex items-center justify-between gap-2">
            <h2 className="text-[15px] font-semibold text-fg-0">{t('budget.title')}</h2>
            {canWriteHere && (
              <button type="button" onClick={() => setManagingBudget(true)} className="shrink-0 text-sm font-semibold text-accent">
                {t('budget.manage')}
              </button>
            )}
          </div>
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
                  <button type="button" onClick={() => setViewingCategoryId(line.categoryId)}
                    className="w-full rounded-lg text-left transition-colors hover:bg-bg-2">
                    <div className="flex items-center justify-between text-sm">
                      <span className="flex items-center gap-1.5 font-medium text-fg-1">
                        {(over || warning) && <AlertTriangle size={13} className={over ? 'text-status-red' : 'text-status-orange'} />}
                        {category?.label ?? line.categoryId}
                      </span>
                      <span className="text-fg-2">{formatAmount(line.spent)} / {formatAmount(line.monthlyLimit)}</span>
                    </div>
                    <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-bg-2">
                      <div className="h-2 rounded-full transition-all" style={{ width: `${percent}%`, backgroundColor: barColor }} />
                    </div>
                    <p className={`mt-1 text-xs ${over ? 'text-status-red' : 'text-fg-3'}`}>
                      {over ? t('budget.over', { amount: formatAmount(line.spent - line.monthlyLimit) }) : t('budget.remaining', { amount: formatAmount(line.monthlyLimit - line.spent) })}
                    </p>
                  </button>
                </li>
              )
            })}
          </ul>
        </section>
      </div>

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

      {!spaceIsPersonal && (
        <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
          <h2 className="flex items-center gap-1.5 text-[15px] font-semibold text-fg-0">
            <HandCoins size={16} className="text-fg-3" /> {t('balances.title')}
          </h2>
          <p className="mb-4 text-[13px] text-fg-3">{t('balances.subtitle')}</p>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6">
            <ul className="space-y-1">
              {balances?.netByMember.map((row) => {
                const paid = paidByMember.get(row.memberId) ?? 0
                const statusKey = row.net >= 0.01 ? 'status_owed' : row.net <= -0.01 ? 'status_owes' : 'status_settled'
                const netColor = row.net >= 0.01 ? 'text-status-green' : row.net <= -0.01 ? 'text-status-red' : 'text-fg-3'
                return (
                  <li key={row.memberId}>
                    <button type="button" onClick={() => setViewingMemberId(row.memberId)}
                      className="flex w-full items-center gap-2.5 rounded-lg p-1.5 text-left transition-colors hover:bg-bg-2">
                      <UserAvatar username={memberLabel(row.memberId)} role="USER" className="size-8 shrink-0 rounded-full text-xs" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-[13.5px] font-medium text-fg-0">
                          {memberLabel(row.memberId)} <span className="font-normal text-fg-3">· {t('balances.paid_prefix', { amount: formatAmount(paid) })}</span>
                        </p>
                        <p className="text-xs text-fg-3">{t(`balances.${statusKey}`)}</p>
                      </div>
                      <span className={`shrink-0 text-[14.5px] font-semibold ${netColor}`}>
                        {row.net > 0 ? '+ ' : row.net < 0 ? '– ' : ''}{formatAmount(Math.abs(row.net))}
                      </span>
                    </button>
                  </li>
                )
              })}
            </ul>

            <div className="flex flex-col gap-2 sm:border-l sm:border-border sm:pl-6">
              {(balances?.suggestedTransfers.length ?? 0) === 0 ? (
                <div className="flex h-full min-h-20 items-center justify-center gap-2 text-sm text-fg-3">
                  <CheckCircle2 size={18} className="text-status-green" /> {t('balances.all_settled')}
                </div>
              ) : (
                balances?.suggestedTransfers.map((transfer, i) => (
                  <div key={i} className="flex items-center gap-2.5 rounded-[11px] bg-bg-2 p-2.5">
                    <UserAvatar username={memberLabel(transfer.fromMemberId)} role="USER" className="size-7 shrink-0 rounded-full text-[10.5px]" />
                    <ArrowRight size={16} className="shrink-0 text-fg-3" />
                    <UserAvatar username={memberLabel(transfer.toMemberId)} role="USER" className="size-7 shrink-0 rounded-full text-[10.5px]" />
                    <button type="button"
                      onClick={() => setViewingHistoryBetween({ memberAId: transfer.fromMemberId, memberBId: transfer.toMemberId })}
                      className="min-w-0 flex-1 truncate rounded text-left text-sm text-fg-1 hover:underline">
                      {memberLabel(transfer.fromMemberId)} → {memberLabel(transfer.toMemberId)}
                    </button>
                    <span className="shrink-0 text-sm font-semibold text-fg-0">{formatAmount(transfer.amount)}</span>
                    {(transfer.fromMemberId === currentUserId || transfer.toMemberId === currentUserId) && (
                      <button type="button" onClick={() => setSettlingTransfer(transfer)}
                        className="shrink-0 rounded-[8px] bg-accent px-2.5 py-1 text-xs font-semibold text-white">{t('balances.settle')}</button>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
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

      <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-x-2 gap-y-1">
          <h2 className="text-[15px] font-semibold text-fg-0">{t('transactions.title')}</h2>
          {canWriteHere && (
            <button type="button" onClick={() => setManagingRecurringSeries(true)} className="shrink-0 text-sm font-semibold text-accent">
              {t('recurring_series.manage')}
            </button>
          )}
        </div>
        <ul className="divide-y divide-border">
          {(transactions ?? []).map((transaction) => {
            const category = categoryById.get(transaction.categoryId)
            const payer = members?.find((m) => m.userId === transaction.payerId)
            return (
              <li key={transaction.id} className="flex flex-col gap-1.5 py-3 sm:flex-row sm:items-center sm:gap-3">
                <button type="button" onClick={() => setViewingTransaction(transaction)}
                  className="flex min-w-0 flex-1 items-center gap-3 rounded-lg text-left transition-colors hover:bg-bg-2">
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
                </button>
                {canWriteHere && (
                  <span className="flex shrink-0 items-center justify-end gap-0.5 self-end sm:self-auto">
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

      {viewingCategoryId && (
        <CategoryTransactionsModal
          category={categoryById.get(viewingCategoryId)}
          transactions={(transactions ?? []).filter((transaction) => transaction.categoryId === viewingCategoryId)}
          members={members ?? []}
          onSelectTransaction={setViewingTransaction}
          onClose={() => setViewingCategoryId(null)}
        />
      )}

      {viewingTransaction && (
        <TransactionDetailModal
          transaction={viewingTransaction}
          category={categoryById.get(viewingTransaction.categoryId)}
          members={members ?? []}
          onClose={() => setViewingTransaction(null)}
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

      {managingBudget && (
        <BudgetManagerModal
          categories={categories ?? []}
          budgetLines={stats?.budgetVsActual ?? []}
          onSave={(categoryId, monthlyLimit) => setBudget.mutate({ categoryId, monthlyLimit })}
          onClose={() => setManagingBudget(false)}
        />
      )}

      {managingRecurringSeries && (
        <RecurringSeriesManagerModal
          series={recurringSeries ?? []}
          onEdit={(series) => setEditingSeries(series)}
          onDelete={(seriesId) => setDeletingSeries((recurringSeries ?? []).find((s) => s.id === seriesId) ?? null)}
          onClose={() => setManagingRecurringSeries(false)}
        />
      )}

      {editingSeries && (
        <RecurringSeriesFormModal
          series={editingSeries}
          categories={categories ?? []}
          members={members ?? []}
          canPickContributors={!spaceIsPersonal}
          onSubmit={handleUpdateSeriesSubmit}
          onCancel={() => setEditingSeries(null)}
        />
      )}

      {deletingSeries && (
        <DeleteTransactionModal
          label={deletingSeries.label}
          isPending={deleteRecurringSeries.isPending}
          error={deleteRecurringSeries.isError ? 'error' : null}
          onCancel={() => setDeletingSeries(null)}
          onConfirm={() => deleteRecurringSeries.mutate(deletingSeries.id, { onSuccess: () => setDeletingSeries(null) })}
        />
      )}

      {settlingTransfer && (
        <SettleDebtModal
          fromLabel={memberLabel(settlingTransfer.fromMemberId)}
          toLabel={memberLabel(settlingTransfer.toMemberId)}
          amount={settlingTransfer.amount}
          isPending={settleDebt.isPending}
          onCancel={() => setSettlingTransfer(null)}
          onConfirm={(amount, date) => settleDebt.mutate(
            { fromMemberId: settlingTransfer.fromMemberId, toMemberId: settlingTransfer.toMemberId, amount, date },
            { onSuccess: () => setSettlingTransfer(null) }
          )}
        />
      )}

      {viewingMemberId && (
        <MemberTransactionsModal
          memberLabel={memberLabel(viewingMemberId)}
          transactions={(transactions ?? []).filter((transaction) => transaction.payerId === viewingMemberId)}
          onSelectTransaction={setViewingTransaction}
          onClose={() => setViewingMemberId(null)}
        />
      )}

      {viewingHistoryBetween && (
        <SettlementHistoryModal
          settlements={settlementsBetween ?? []}
          memberLabel={memberLabel}
          onClose={() => setViewingHistoryBetween(null)}
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
