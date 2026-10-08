import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { Wallet } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import { formatAmount, resolveLocale } from '@/shared/lib'
import type { BudgetWatch, FinanceCard as FinanceCardData, MemberBalance, UpcomingOperation } from '@/entities/dashboard'
import { UserAvatar } from '@/entities/user'
import { useDashboardActions } from '../model/dashboardActions'
import { formatMonthName, formatShortDay } from '../lib/dates'
import { DashboardCard } from './DashboardCard'
import { CardRow, RowLead } from './CardRow'
import { CardGroup } from './CardGroup'
import { SmallButton } from './cardParts'

function signed(amount: number): string {
  return amount > 0 ? `+${formatAmount(amount)}` : formatAmount(amount)
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 rounded-xl border border-border bg-bg-2 px-3 py-2">
      <p className="truncate text-xs text-fg-3">{label}</p>
      <p className="whitespace-nowrap text-[17px] font-semibold tabular-nums text-fg-0">{value}</p>
    </div>
  )
}

function BudgetRow({ budget }: { budget: BudgetWatch }) {
  const over = budget.status === 'OVER'
  // A 0 € budget is a deliberate "spend nothing here": any spending is a full overrun, never NaN%.
  const percent = budget.limit > 0 ? Math.min(100, Math.round((budget.spent / budget.limit) * 100)) : 100
  return (
    <CardRow
      title={<span className="flex items-baseline justify-between gap-3">
        <span className="truncate">{budget.label}</span>
        <span className={`shrink-0 font-semibold tabular-nums ${over ? 'text-status-red' : 'text-status-orange'}`}>
          {formatAmount(budget.spent)} / {formatAmount(budget.limit)}
        </span>
      </span>}
      extra={<div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-bg-3">
        <span data-testid={`budget-bar-${budget.categoryId}`}
          className={`block h-full rounded-full ${over ? 'bg-status-red' : 'bg-status-orange'}`} style={{ width: `${percent}%` }} />
      </div>} />
  )
}

function UpcomingRow({ operation, locale }: { operation: UpcomingOperation; locale: string }) {
  const income = operation.type === 'INCOME'
  return (
    <CardRow lead={<RowLead className="w-[58px]">{formatShortDay(operation.date, locale)}</RowLead>} title={operation.label}
      trail={<span className={`text-sm font-semibold tabular-nums ${income ? 'text-status-green' : 'text-fg-1'}`}>
        {income ? '+' : '−'}{formatAmount(operation.amount)}
      </span>} />
  )
}

function BalanceRow({ balance }: { balance: MemberBalance }) {
  const { t } = useTranslation('dashboard')
  const { canWrite, memberName, settle } = useDashboardActions()
  const name = memberName(balance.memberId)
  const amount = formatAmount(balance.amount)
  const iOwe = balance.direction === 'I_OWE'
  return (
    <CardRow lead={<UserAvatar userId={balance.memberId} username={name} className="size-6 rounded-full text-[10px]" />}
      title={iOwe ? t('finance.i_owe', { amount, name }) : t('finance.owes_me', { name, amount })}
      trail={iOwe && canWrite
        ? <SmallButton onClick={() => settle({ toMemberId: balance.memberId, amount: balance.amount })}>{t('finance.settle')}</SmallButton>
        : undefined} />
  )
}

/**
 * The month at a glance, then only what deserves a look: budgets at 80 % or more, the recurring
 * operations of the next seven days, and who owes whom. Full width on desktop — money is the module
 * the household put first — with its three groups side by side, stacked on a phone.
 */
export function FinanceCard({ card }: { card: FinanceCardData }) {
  const { t } = useTranslation('dashboard')
  const { spaceId } = useDashboardActions()
  const locale = resolveLocale(i18next.language)
  const balances = card.balances ?? []

  return (
    <DashboardCard icon={Wallet} title={t('finance.title')}
      aside={<span className="text-[13px] font-medium text-fg-3">{formatMonthName(card.month, locale)}</span>}
      link={{ to: ROUTES.spaceFinance(spaceId), label: t('finance.link') }}>
      <div className="mb-1.5 mt-0.5 grid grid-cols-2 gap-2 @lg:grid-cols-4">
        <Stat label={t('finance.balance')} value={signed(card.balance)} />
        <Stat label={t('finance.spent')} value={formatAmount(card.totalExpense)} />
        <Stat label={t('finance.income')} value={formatAmount(card.totalIncome)} />
        <Stat label={t('finance.remaining_budget')} value={formatAmount(card.remainingBudget)} />
      </div>
      <div className="grid grid-cols-[repeat(auto-fit,minmax(min(240px,100%),1fr))] gap-x-7 gap-y-2.5">
        {card.budgetsToWatch.length > 0 && (
          <CardGroup flush title={t('finance.budgets_to_watch')}>
            {card.budgetsToWatch.map((budget) => <BudgetRow key={budget.categoryId} budget={budget} />)}
          </CardGroup>
        )}
        {card.upcoming.length > 0 && (
          <CardGroup flush title={t('finance.upcoming')}>
            {card.upcoming.map((operation) => <UpcomingRow key={`${operation.seriesId}:${operation.date}`} operation={operation} locale={locale} />)}
          </CardGroup>
        )}
        {balances.length > 0 && (
          <CardGroup flush title={t('finance.balances')}>
            {balances.map((balance) => <BalanceRow key={`${balance.memberId}:${balance.direction}`} balance={balance} />)}
          </CardGroup>
        )}
      </div>
    </DashboardCard>
  )
}
