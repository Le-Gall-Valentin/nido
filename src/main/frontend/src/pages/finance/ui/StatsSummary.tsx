import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Wallet, TrendingDown, TrendingUp, PiggyBank, Pencil } from 'lucide-react'
import type { FinanceStats } from '@/entities/finance'
import { formatAmount, formatSignedAmount } from '@/shared/lib'

function StatCard({ icon: Icon, tintClassName, label, value, detail, onClick, action }: {
  icon: typeof Wallet; tintClassName: string; label: string; value: string; detail?: string; onClick?: () => void
  /** A control of its own, on top of the card's button — never inside it. */
  action?: ReactNode
}) {
  const content = (
    <div className="pointer-events-none relative flex min-w-0 flex-1 items-center gap-3 p-4">
      <div className={`grid size-9 shrink-0 place-items-center rounded-[10px] ${tintClassName}`}>
        <Icon size={18} />
      </div>
      <div className="min-w-0">
        <p className="truncate text-[13px] text-fg-3">{label}</p>
        <p className="text-[21px] font-semibold text-fg-0">{value}</p>
        {detail && <p className="truncate text-xs text-fg-3">{detail}</p>}
      </div>
    </div>
  )

  if (!onClick) {
    return <div className="flex items-start rounded-2xl border border-border bg-bg-1">{content}{action}</div>
  }
  // The button covers the whole card, the strip under the action included; the action sits on top of it.
  return (
    <div className="relative flex items-start rounded-2xl border border-border bg-bg-1 transition-colors hover:bg-bg-2">
      <button type="button" onClick={onClick} aria-label={label} className="absolute inset-0 rounded-2xl" />
      {content}
      {action && <div className="relative">{action}</div>}
    </div>
  )
}

/** Whether an ISO date (YYYY-MM-DD) is the last day of its month. */
function isMonthEnd(isoDate: string): boolean {
  const nextDay = new Date(`${isoDate}T00:00:00Z`)
  nextDay.setUTCDate(nextDay.getUTCDate() + 1)
  return nextDay.getUTCDate() === 1
}

interface StatsSummaryProps {
  stats?: FinanceStats
  /** The household's current month shows today's balance; any other, the balance at its end. Names it while it loads. */
  isCurrentMonth: boolean
  onSelectExpenseBreakdown?: () => void
  onSelectIncomeBreakdown?: () => void
  onSelectOperations?: () => void
  /** Given only to someone who may write: opens the opening balance. */
  onAdjustBalance?: () => void
}

export function StatsSummary({
  stats, isCurrentMonth, onSelectExpenseBreakdown, onSelectIncomeBreakdown, onSelectOperations, onAdjustBalance,
}: StatsSummaryProps) {
  const { t } = useTranslation('finance')
  // Once loaded, the day the server dates the balance names it: today's on the household's calendar, or a month's end.
  const balanceIsToday = stats ? !isMonthEnd(stats.balanceAsOf) : isCurrentMonth

  return (
    <div className="grid grid-cols-[repeat(auto-fit,minmax(min(210px,100%),1fr))] gap-3">
      <StatCard icon={Wallet} tintClassName="bg-accent-dim text-accent"
        label={t(balanceIsToday ? 'stats.balance_today' : 'stats.balance_end_of_month')}
        value={formatAmount(stats?.balance ?? 0)}
        detail={stats ? t('stats.month_net', { amount: formatSignedAmount(stats.monthNet) }) : undefined}
        onClick={onSelectOperations}
        action={onAdjustBalance && (
          <button type="button" aria-label={t('stats.adjust')} title={t('stats.adjust')} onClick={onAdjustBalance}
            className="m-2 grid size-8 shrink-0 place-items-center rounded-md text-fg-3 hover:bg-bg-3 hover:text-fg-1">
            <Pencil size={14} />
          </button>
        )} />
      <StatCard icon={TrendingDown} tintClassName="bg-status-red-dim text-status-red" label={t('stats.spent')} value={formatAmount(stats?.totalExpense ?? 0)} onClick={onSelectExpenseBreakdown} />
      <StatCard icon={TrendingUp} tintClassName="bg-status-green-dim text-status-green" label={t('stats.income')} value={formatAmount(stats?.totalIncome ?? 0)} onClick={onSelectIncomeBreakdown} />
      <StatCard icon={PiggyBank} tintClassName="bg-status-blue-dim text-status-blue" label={t('stats.remaining_budget')} value={formatAmount(stats?.remainingBudget ?? 0)} />
    </div>
  )
}
