import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Spinner } from '@/shared/ui'
import { useAuth } from '@/features/auth'
import { useMySpaces, useSpaceTimezone } from '@/features/space-switcher'
import { monthIso } from '@/shared/lib'
import { canWrite, isPersonal, useSpaceMembers } from '@/entities/space'
import {
  financeApi, FinanceApiProvider, useCategories, useTransactions, useFinanceStats, useProjection, useRecurringSeries,
  type IFinanceApi, type Transaction, type TransactionType,
} from '@/entities/finance'
import { FinanceHeader } from './FinanceHeader'
import { StatsSummary } from './StatsSummary'
import { SpendingBreakdownSection } from './SpendingBreakdownSection'
import { CategoryBreakdownModal } from './CategoryBreakdownModal'
import { BudgetSection } from './BudgetSection'
import { ProjectionSection } from './ProjectionSection'
import { TransactionsSection } from './TransactionsSection'
import { TransactionDetailModal } from './TransactionDetailModal'
import { CategoryTransactionsModal } from './CategoryTransactionsModal'
import { TransactionFormPanel } from './TransactionFormPanel'
import { DeleteTransactionPanel } from './DeleteTransactionPanel'
import { CategoryManagerPanel } from './CategoryManagerPanel'
import { BudgetManagerPanel } from './BudgetManagerPanel'
import { FinanceRecurringSeriesPanel } from '@/widgets/finance-recurring-series'
import { SavingsGoalsPanel } from './SavingsGoalsPanel'
import { BalancesPanel } from './BalancesPanel'

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

/**
 * The month the page opens on, on the household's calendar. On the last day of a month at half past
 * eight in the evening in Toronto, Paris is already the first of the next one — and the household's
 * figures are the new month's.
 */
function currentMonth(timezone?: string): string {
  return monthIso(new Date(), timezone)
}

function FinancePageContent() {
  const { t } = useTranslation('finance')
  const { spaceId = '' } = useParams<{ spaceId: string }>()
  const currentUserId = useAuth((s) => s.user)?.id ?? null
  const spaceTimezone = useSpaceTimezone(spaceId)
  const [month, setMonth] = useState(() => currentMonth(spaceTimezone))
  const { data: categories, isPending: categoriesPending, isError: categoriesError } = useCategories(spaceId)
  const { data: transactions, isPending, isError } = useTransactions(spaceId, month)
  const { data: stats } = useFinanceStats(spaceId, month)
  const { data: projection } = useProjection(spaceId, month)
  const { data: recurringSeries } = useRecurringSeries(spaceId)
  const { data: members } = useSpaceMembers(spaceId)
  const { data: mySpaces } = useMySpaces()

  const [formState, setFormState] = useState<{ mode: 'create' } | { mode: 'edit'; transaction: Transaction } | null>(null)
  const [deletingTransaction, setDeletingTransaction] = useState<Transaction | null>(null)
  const [viewingTransaction, setViewingTransaction] = useState<Transaction | null>(null)
  const [viewingCategoryId, setViewingCategoryId] = useState<string | null>(null)
  const [breakdownModalType, setBreakdownModalType] = useState<TransactionType | null>(null)
  const [managingCategories, setManagingCategories] = useState(false)
  const [managingBudget, setManagingBudget] = useState(false)
  const [managingRecurringSeries, setManagingRecurringSeries] = useState(false)

  function memberLabel(memberId: string): string {
    return members?.find((m) => m.userId === memberId)?.username ?? memberId
  }

  const currentSpace = mySpaces?.find((s) => s.id === spaceId)
  const canWriteHere = currentSpace ? canWrite(currentSpace.myRole) : false
  const spaceIsPersonal = currentSpace ? isPersonal(currentSpace) : false

  const categoryById = new Map((categories ?? []).map((c) => [c.id, c]))


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

      <StatsSummary
        stats={stats}
        onSelectExpenseBreakdown={() => setBreakdownModalType('EXPENSE')}
        onSelectIncomeBreakdown={() => setBreakdownModalType('INCOME')}
        onSelectOperations={() => document.getElementById('finance-operations')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
      />

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
        <BalancesPanel
          spaceId={spaceId}
          spaceTimezone={spaceTimezone}
          transactions={transactions ?? []}
          currentUserId={currentUserId}
          memberLabel={memberLabel}
          onSelectTransaction={setViewingTransaction}
        />
      )}

      {!spaceIsPersonal && (
        <SavingsGoalsPanel
          spaceId={spaceId}
          spaceTimezone={spaceTimezone}
          members={members ?? []}
          canWrite={canWriteHere}
          memberLabel={memberLabel}
        />
      )}

      <div id="finance-operations">
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
      </div>

      {formState && (
        <TransactionFormPanel
          spaceId={spaceId}
          spaceTimezone={spaceTimezone}
          transaction={formState.mode === 'edit' ? formState.transaction : null}
          categories={categories ?? []}
          members={members ?? []}
          canPickContributors={!spaceIsPersonal}
          currentUserId={currentUserId}
          onClose={() => setFormState(null)}
        />
      )}

      {breakdownModalType && (
        <CategoryBreakdownModal
          type={breakdownModalType}
          breakdown={stats?.breakdown ?? []}
          categoryById={categoryById}
          onSelectCategory={(categoryId) => {
            setViewingCategoryId(categoryId)
            setBreakdownModalType(null)
          }}
          onClose={() => setBreakdownModalType(null)}
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
        <DeleteTransactionPanel
          spaceId={spaceId}
          transaction={deletingTransaction}
          onClose={() => setDeletingTransaction(null)}
        />
      )}

      {managingCategories && (
        <CategoryManagerPanel
          spaceId={spaceId}
          categories={categories ?? []}
          onClose={() => setManagingCategories(false)}
        />
      )}

      {managingBudget && (
        <BudgetManagerPanel
          spaceId={spaceId}
          categories={categories ?? []}
          budgetLines={stats?.budgetVsActual ?? []}
          onClose={() => setManagingBudget(false)}
        />
      )}

      {managingRecurringSeries && (
        <FinanceRecurringSeriesPanel
          spaceId={spaceId}
          series={recurringSeries ?? []}
          categories={categories ?? []}
          members={members ?? []}
          canPickContributors={!spaceIsPersonal}
          onClose={() => setManagingRecurringSeries(false)}
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
