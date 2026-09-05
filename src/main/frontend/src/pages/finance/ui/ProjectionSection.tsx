import { useTranslation } from 'react-i18next'
import { Target } from 'lucide-react'
import type { Projection } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

interface ProjectionSectionProps {
  projection?: Projection
}

export function ProjectionSection({ projection }: ProjectionSectionProps) {
  const { t } = useTranslation('finance')

  return (
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
  )
}
