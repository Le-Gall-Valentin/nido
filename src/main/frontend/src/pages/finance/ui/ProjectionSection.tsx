import { useTranslation } from 'react-i18next'
import { Target } from 'lucide-react'
import type { Projection } from '@/entities/finance'
import { formatAmount, formatIsoDate, formatSignedAmount } from '@/shared/lib'

interface ProjectionSectionProps {
  projection?: Projection
  /** A month after the current one: its estimate counts the recurring operations of the months before it, unlisted. */
  isLaterMonth?: boolean
}

export function ProjectionSection({ projection, isLaterMonth = false }: ProjectionSectionProps) {
  const { t } = useTranslation('finance')

  return (
    <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
      <h2 className="mb-3 flex items-center gap-1.5 text-[15px] font-semibold text-fg-0">
        <Target size={16} className="text-fg-3" /> {t('projection.title')}
      </h2>
      <p className="text-sm text-fg-1">
        {t('projection.end_of_month_balance', { amount: formatAmount(projection?.projectedEndOfMonthBalance ?? 0) })}
      </p>
      {isLaterMonth && <p className="mt-1 text-xs text-fg-3">{t('projection.includes_earlier_months')}</p>}
      {(projection?.upcoming.length ?? 0) > 0 && (
        <ul className="mt-2 space-y-1 text-sm text-fg-3">
          {projection?.upcoming.map((occurrence) => (
            <li key={`${occurrence.seriesId}-${occurrence.date}`}>
              {formatIsoDate(occurrence.date)} — {occurrence.label} ({formatSignedAmount(occurrence.type === 'EXPENSE' ? -occurrence.amount : occurrence.amount)})
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
