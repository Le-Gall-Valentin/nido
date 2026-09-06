import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, ConfirmDeleteModal, Spinner } from '@/shared/ui'
import { useAuth } from '@/features/auth'
import { useMySpaces } from '@/features/space-switcher'
import { canWrite, isPersonal, useSpaceMembers } from '@/entities/space'
import {
  financeApi, FinanceApiProvider, useCategories, useTransactions, useFinanceStats, useProjection,
  useCreateTransaction, useCreateRecurringSeries, useUpdateTransaction, useDeleteTransaction, useSetBudget,
  useCreateCategory, useUpdateCategory, useDeleteCategory, useBalances, useSettleDebt, useSettlementsBetween,
  useSavingsGoals, useCreateSavingsGoal, useUpdateSavingsGoal, useDeleteSavingsGoal, useAddSavingsContribution,
  useRecurringSeries, useUpdateRecurringSeries, useDeleteRecurringSeries,
  type IFinanceApi, type Transaction, type SavingsGoal, type RecurringSeries,
} from '@/entities/finance'
import { FinanceHeader } from './FinanceHeader'
import { StatsSummary } from './StatsSummary'
import { SpendingBreakdownSection } from './SpendingBreakdownSection'
import { BudgetSection } from './BudgetSection'
import { ProjectionSection } from './ProjectionSection'
import { BalancesSection } from './BalancesSection'
import { SavingsGoalsSection } from './SavingsGoalsSection'
import { TransactionsSection } from './TransactionsSection'
import { TransactionFormModal, type TransactionFormInput } from './TransactionFormModal'
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
import { SavingsGoalContributionsModal } from './SavingsGoalContributionsModal'

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

  const createTransaction = useCreateTransaction(spaceId)
  const createRecurringSeries = useCreateRecurringSeries(spaceId)
  const updateRecurringSeries = useUpdateRecurringSeries(spaceId)
  const deleteRecurringSeries = useDeleteRecurringSeries(spaceId)
  const updateTransaction = useUpdateTransaction(spaceId)
  const deleteTransaction = useDeleteTransaction(spaceId)
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
  const [viewingTransaction, setViewingTransaction] = useState<Transaction | null>(null)
  const [viewingCategoryId, setViewingCategoryId] = useState<string | null>(null)
  const [managingCategories, setManagingCategories] = useState(false)
  const [managingBudget, setManagingBudget] = useState(false)
  const [managingRecurringSeries, setManagingRecurringSeries] = useState(false)
  const [editingSeries, setEditingSeries] = useState<RecurringSeries | null>(null)
  const [deletingSeries, setDeletingSeries] = useState<RecurringSeries | null>(null)
  const [deletingCategoryId, setDeletingCategoryId] = useState<string | null>(null)
  const [categoryDeleteError, setCategoryDeleteError] = useState<string | null>(null)
  const [settlingTransfer, setSettlingTransfer] = useState<{ fromMemberId: string; toMemberId: string; amount: number } | null>(null)
  const [viewingMemberId, setViewingMemberId] = useState<string | null>(null)
  const [viewingHistoryBetween, setViewingHistoryBetween] = useState<{ memberAId: string; memberBId: string } | null>(null)
  const [goalFormState, setGoalFormState] = useState<{ mode: 'create' } | { mode: 'edit'; goal: SavingsGoal } | null>(null)
  const [contributingGoal, setContributingGoal] = useState<SavingsGoal | null>(null)
  const [viewingGoal, setViewingGoal] = useState<SavingsGoal | null>(null)
  const [deletingGoal, setDeletingGoal] = useState<SavingsGoal | null>(null)

  const { data: settlementsBetween } = useSettlementsBetween(spaceId, viewingHistoryBetween?.memberAId, viewingHistoryBetween?.memberBId)

  function handleDeleteCategory(categoryId: string) {
    setCategoryDeleteError(null)
    setDeletingCategoryId(categoryId)
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

  if (isPending || categoriesPending) return <Spinner label={t('loading')} fullscreen={false} />
  if (isError || categoriesError) return <Alert variant="error">{t('error.load_failed')}</Alert>

  return (
    <div className="mx-auto max-w-[1100px] px-5 py-6 md:px-10 md:py-[34px]">
      <FinanceHeader
        month={month}
        onMonthChange={setMonth}
        canWrite={canWriteHere}
        onManageCategories={() => setManagingCategories(true)}
        onNewTransaction={() => setFormState({ mode: 'create' })}
      />

      <StatsSummary stats={stats} />

      <div className="mt-4 grid grid-cols-[repeat(auto-fit,minmax(min(360px,100%),1fr))] gap-4">
        <SpendingBreakdownSection
          breakdown={stats?.breakdown ?? []}
          categoryById={categoryById}
          onSelectCategory={setViewingCategoryId}
        />
        <BudgetSection
          budgetVsActual={stats?.budgetVsActual ?? []}
          categoryById={categoryById}
          canWrite={canWriteHere}
          onManageBudget={() => setManagingBudget(true)}
          onSelectCategory={setViewingCategoryId}
        />
      </div>

      <ProjectionSection projection={projection} />

      {!spaceIsPersonal && (
        <BalancesSection
          balances={balances}
          paidByMember={paidByMember}
          memberLabel={memberLabel}
          currentUserId={currentUserId}
          onSelectMember={setViewingMemberId}
          onSelectHistory={setViewingHistoryBetween}
          onSettle={setSettlingTransfer}
        />
      )}

      {!spaceIsPersonal && (
        <SavingsGoalsSection
          savingsGoals={savingsGoals ?? []}
          canWrite={canWriteHere}
          memberLabel={memberLabel}
          onCreate={() => setGoalFormState({ mode: 'create' })}
          onEdit={(goal) => setGoalFormState({ mode: 'edit', goal })}
          onDelete={(goalId) => setDeletingGoal((savingsGoals ?? []).find((g) => g.id === goalId) ?? null)}
          onContribute={setContributingGoal}
          onView={setViewingGoal}
        />
      )}

      <TransactionsSection
        transactions={transactions ?? []}
        categoryById={categoryById}
        members={members ?? []}
        canWrite={canWriteHere}
        onManageRecurring={() => setManagingRecurringSeries(true)}
        onSelectTransaction={setViewingTransaction}
        onEdit={(transaction) => setFormState({ mode: 'edit', transaction })}
        onDelete={setDeletingTransaction}
      />

      {formState && (
        <TransactionFormModal
          mode={formState.mode}
          transaction={formState.mode === 'edit' ? formState.transaction : undefined}
          categories={categories ?? []}
          members={members ?? []}
          canPickContributors={!spaceIsPersonal}
          currentUserId={currentUserId}
          onSubmit={handleFormSubmit}
          onCancel={() => {
            setFormState(null)
            createTransaction.reset()
            updateTransaction.reset()
            createRecurringSeries.reset()
          }}
          submitError={(createTransaction.isError || updateTransaction.isError || createRecurringSeries.isError) ? t('form.submit_error') : null}
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

      {deletingTransaction && (
        <ConfirmDeleteModal
          title={t('delete_confirm.title', { label: deletingTransaction.label })}
          message={t('delete_confirm.message')}
          confirmLabel={t('delete_confirm.confirm')}
          cancelLabel={t('delete_confirm.cancel')}
          isPending={deleteTransaction.isPending}
          error={deleteTransaction.isError ? t('delete_confirm.error') : null}
          onCancel={() => {
            setDeletingTransaction(null)
            deleteTransaction.reset()
          }}
          onConfirm={() => deleteTransaction.mutate(deletingTransaction.id, { onSuccess: () => setDeletingTransaction(null) })}
        />
      )}

      {managingCategories && (
        <CategoryManagerModal
          categories={categories ?? []}
          onCreate={(label, color, icon) => createCategory.mutate({ label, color, icon })}
          onUpdate={(categoryId, label, color, icon) => updateCategory.mutate({ categoryId, label, color, icon })}
          onDelete={handleDeleteCategory}
          onClose={() => {
            setManagingCategories(false)
            createCategory.reset()
            updateCategory.reset()
          }}
          deleteError={categoryDeleteError}
          submitError={(createCategory.isError || updateCategory.isError) ? t('form.submit_error') : null}
        />
      )}

      {deletingCategoryId && (
        <ConfirmDeleteModal
          title={t('delete_confirm.title', { label: categoryById.get(deletingCategoryId)?.label ?? '' })}
          message={t('delete_confirm.message')}
          confirmLabel={t('delete_confirm.confirm')}
          cancelLabel={t('delete_confirm.cancel')}
          isPending={deleteCategory.isPending}
          error={categoryDeleteError ? t('categories.delete_in_use') : null}
          onCancel={() => {
            setDeletingCategoryId(null)
            setCategoryDeleteError(null)
            deleteCategory.reset()
          }}
          onConfirm={() => deleteCategory.mutate(deletingCategoryId, {
            onSuccess: () => setDeletingCategoryId(null),
            onError: () => setCategoryDeleteError('in_use'),
          })}
        />
      )}

      {managingBudget && (
        <BudgetManagerModal
          categories={categories ?? []}
          budgetLines={stats?.budgetVsActual ?? []}
          onSave={(categoryId, monthlyLimit) => setBudget.mutate({ categoryId, monthlyLimit })}
          onClose={() => {
            setManagingBudget(false)
            setBudget.reset()
          }}
          submitError={setBudget.isError ? t('form.submit_error') : null}
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
          onCancel={() => {
            setEditingSeries(null)
            updateRecurringSeries.reset()
          }}
          submitError={updateRecurringSeries.isError ? t('form.submit_error') : null}
        />
      )}

      {deletingSeries && (
        <ConfirmDeleteModal
          title={t('delete_confirm.title', { label: deletingSeries.label })}
          message={t('delete_confirm.message')}
          confirmLabel={t('delete_confirm.confirm')}
          cancelLabel={t('delete_confirm.cancel')}
          isPending={deleteRecurringSeries.isPending}
          error={deleteRecurringSeries.isError ? t('delete_confirm.error') : null}
          onCancel={() => {
            setDeletingSeries(null)
            deleteRecurringSeries.reset()
          }}
          onConfirm={() => deleteRecurringSeries.mutate(deletingSeries.id, { onSuccess: () => setDeletingSeries(null) })}
        />
      )}

      {deletingGoal && (
        <ConfirmDeleteModal
          title={t('delete_confirm.title', { label: deletingGoal.name })}
          message={t('delete_confirm.message')}
          confirmLabel={t('delete_confirm.confirm')}
          cancelLabel={t('delete_confirm.cancel')}
          isPending={deleteSavingsGoal.isPending}
          error={deleteSavingsGoal.isError ? t('delete_confirm.error') : null}
          onCancel={() => {
            setDeletingGoal(null)
            deleteSavingsGoal.reset()
          }}
          onConfirm={() => deleteSavingsGoal.mutate(deletingGoal.id, { onSuccess: () => setDeletingGoal(null) })}
        />
      )}

      {settlingTransfer && (
        <SettleDebtModal
          fromLabel={memberLabel(settlingTransfer.fromMemberId)}
          toLabel={memberLabel(settlingTransfer.toMemberId)}
          amount={settlingTransfer.amount}
          isPending={settleDebt.isPending}
          onCancel={() => {
            setSettlingTransfer(null)
            settleDebt.reset()
          }}
          onConfirm={(amount, date) => settleDebt.mutate(
            { fromMemberId: settlingTransfer.fromMemberId, toMemberId: settlingTransfer.toMemberId, amount, date },
            { onSuccess: () => setSettlingTransfer(null) }
          )}
          submitError={settleDebt.isError ? t('form.submit_error') : null}
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
          onCancel={() => {
            setGoalFormState(null)
            createSavingsGoal.reset()
            updateSavingsGoal.reset()
          }}
          submitError={(createSavingsGoal.isError || updateSavingsGoal.isError) ? t('form.submit_error') : null}
        />
      )}

      {contributingGoal && (
        <AddContributionModal
          goalName={contributingGoal.name}
          remaining={contributingGoal.targetAmount - contributingGoal.totalContributed}
          members={members ?? []}
          isPending={addSavingsContribution.isPending}
          onCancel={() => {
            setContributingGoal(null)
            addSavingsContribution.reset()
          }}
          onSubmit={(input) => addSavingsContribution.mutate(
            { goalId: contributingGoal.id, ...input },
            { onSuccess: () => setContributingGoal(null) }
          )}
          submitError={addSavingsContribution.isError ? t('form.submit_error') : null}
        />
      )}

      {viewingGoal && (
        <SavingsGoalContributionsModal
          goalName={viewingGoal.name}
          color={viewingGoal.color}
          glyph={viewingGoal.glyph}
          contributions={viewingGoal.contributions}
          memberLabel={memberLabel}
          onClose={() => setViewingGoal(null)}
        />
      )}

      {/* Rendered last so it always stacks above whichever list modal (category, member) opened it —
          every Dialog shares the same z-index, and later DOM siblings paint on top of earlier ones. */}
      {viewingTransaction && (
        <TransactionDetailModal
          transaction={viewingTransaction}
          category={categoryById.get(viewingTransaction.categoryId)}
          members={members ?? []}
          onClose={() => setViewingTransaction(null)}
        />
      )}
    </div>
  )
}
