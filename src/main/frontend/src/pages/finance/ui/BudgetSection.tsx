import { useTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import type { BudgetLine, Category } from '@/entities/finance'
import { formatAmount } from '@/shared/lib'

interface BudgetSectionProps {
  budgetVsActual: BudgetLine[]
  categoryById: Map<string, Category>
  canWrite: boolean
  onManageBudget: () => void
  onSelectCategory: (categoryId: string) => void
}

export function BudgetSection({ budgetVsActual, categoryById, canWrite, onManageBudget, onSelectCategory }: BudgetSectionProps) {
  const { t } = useTranslation('finance')

  return (
    <section className="rounded-2xl border border-border bg-bg-1 p-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <h2 className="text-[15px] font-semibold text-fg-0">{t('budget.title')}</h2>
        {canWrite && (
          <button type="button" onClick={onManageBudget} className="shrink-0 text-sm font-semibold text-accent">
            {t('budget.manage')}
          </button>
        )}
      </div>
      <ul className="space-y-4">
        {budgetVsActual.map((line) => {
          const category = categoryById.get(line.categoryId)
          // The server decides the status (BudgetLine.status()), 0 € budgets included: this component
          // only draws it. A 0 € cap with anything spent is a full bar.
          const over = line.status === 'OVER'
          const warning = line.status === 'WARNING'
          const percent = line.monthlyLimit > 0
            ? Math.min(100, (line.spent / line.monthlyLimit) * 100)
            : (line.spent > 0 ? 100 : 0)
          const barColor = over ? 'var(--color-status-red)' : warning ? 'var(--color-status-orange)' : (category?.color ?? 'var(--color-accent)')
          return (
            <li key={line.categoryId}>
              <button type="button" onClick={() => onSelectCategory(line.categoryId)}
                className="w-full rounded-lg text-left transition-colors hover:bg-bg-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="flex items-center gap-1.5 font-medium text-fg-1">
                    {(over || warning) && (
                      <AlertTriangle size={13} data-testid={`budget-warning-${line.categoryId}`}
                        className={over ? 'text-status-red' : 'text-status-orange'} />
                    )}
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
  )
}
