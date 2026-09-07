import { useTranslation } from 'react-i18next'
import { Wallet, TrendingDown, TrendingUp, PiggyBank } from 'lucide-react'
import type { FinanceStats } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

function StatCard({ icon: Icon, tintClassName, label, value, onClick }: {
  icon: typeof Wallet; tintClassName: string; label: string; value: string; onClick?: () => void
}) {
  const content = (
    <>
      <div className={`grid size-9 shrink-0 place-items-center rounded-[10px] ${tintClassName}`}>
        <Icon size={18} />
      </div>
      <div className="min-w-0">
        <p className="truncate text-[13px] text-fg-3">{label}</p>
        <p className="text-[21px] font-semibold text-fg-0">{value}</p>
      </div>
    </>
  )

  if (onClick) {
    return (
      <button type="button" onClick={onClick}
        className="flex items-center gap-3 rounded-2xl border border-border bg-bg-1 p-4 text-left transition-colors hover:bg-bg-2">
        {content}
      </button>
    )
  }

  return (
    <div className="flex items-center gap-3 rounded-2xl border border-border bg-bg-1 p-4">
      {content}
    </div>
  )
}

interface StatsSummaryProps {
  stats?: FinanceStats
  onSelectExpenseBreakdown?: () => void
  onSelectIncomeBreakdown?: () => void
  onSelectOperations?: () => void
}

export function StatsSummary({ stats, onSelectExpenseBreakdown, onSelectIncomeBreakdown, onSelectOperations }: StatsSummaryProps) {
  const { t } = useTranslation('finance')

  return (
    <div className="grid grid-cols-[repeat(auto-fit,minmax(min(210px,100%),1fr))] gap-3">
      <StatCard icon={Wallet} tintClassName="bg-accent-dim text-accent" label={t('stats.balance')} value={formatAmount(stats?.balance ?? 0)} onClick={onSelectOperations} />
      <StatCard icon={TrendingDown} tintClassName="bg-status-red-dim text-status-red" label={t('stats.spent')} value={formatAmount(stats?.totalExpense ?? 0)} onClick={onSelectExpenseBreakdown} />
      <StatCard icon={TrendingUp} tintClassName="bg-status-green-dim text-status-green" label={t('stats.income')} value={formatAmount(stats?.totalIncome ?? 0)} onClick={onSelectIncomeBreakdown} />
      <StatCard icon={PiggyBank} tintClassName="bg-status-blue-dim text-status-blue" label={t('stats.remaining_budget')} value={formatAmount(stats?.remainingBudget ?? 0)} />
    </div>
  )
}
