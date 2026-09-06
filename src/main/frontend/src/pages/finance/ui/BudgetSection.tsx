import { useTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import type { BudgetLine, Category } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

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
          // A budget of exactly 0€ is a deliberate "spend nothing here" cap, not "no budget" —
          // any spending against it is an immediate, full overrun (a category with truly no
          // budget never reaches this list at all, since budgetVsActual only ever contains
          // categories that have one explicitly set).
          const ratio = line.monthlyLimit > 0 ? line.spent / line.monthlyLimit : (line.spent > 0 ? Infinity : 0)
          const percent = Math.min(100, ratio * 100)
          const over = ratio > 1
          const warning = ratio >= 0.8 && !over
          const barColor = over ? 'var(--color-status-red)' : warning ? 'var(--color-status-orange)' : (category?.color ?? 'var(--color-accent)')
          return (
            <li key={line.categoryId}>
              <button type="button" onClick={() => onSelectCategory(line.categoryId)}
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
  )
}
