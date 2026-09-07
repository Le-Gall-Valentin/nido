import { useTranslation } from 'react-i18next'
import { Pencil, Trash2 } from 'lucide-react'
import { Dialog } from '@/shared/ui'
import type { RecurringSeries } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

interface RecurringSeriesManagerModalProps {
  series: RecurringSeries[]
  onEdit: (series: RecurringSeries) => void
  onDelete: (seriesId: string) => void
  onClose: () => void
}

export function RecurringSeriesManagerModal({ series, onEdit, onDelete, onClose }: RecurringSeriesManagerModalProps) {
  const { t } = useTranslation('finance')

  return (
    <Dialog open onClose={onClose} title={t('recurring_series.title')} maxWidth="max-w-lg" showCloseButton>
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{t('recurring_series.title')}</h3>

      {series.length === 0 ? (
        <p className="text-sm text-fg-3">{t('recurring_series.empty')}</p>
      ) : (
        <ul className="max-h-96 space-y-1 overflow-y-auto">
          {series.map((s) => (
            <li key={s.id} className="flex items-center gap-3 rounded-[10px] p-2 hover:bg-bg-2">
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-fg-0">{s.label}</p>
                <p className="truncate text-xs text-fg-3">
                  {formatAmount(s.amount)} · {t('form.recurrence_interval_count_label')} {s.intervalCount} {t(`form.interval.${s.intervalType}`)}
                  {s.endDate && <> · {t('recurring_series.end_date_label')}: {s.endDate}</>}
                </p>
              </div>
              <button type="button" aria-label={t('recurring_series.edit')} onClick={() => onEdit(s)}
                className="grid size-7 shrink-0 place-items-center rounded-md text-fg-3 hover:bg-bg-1 hover:text-fg-1">
                <Pencil size={15} />
              </button>
              <button type="button" aria-label={t('recurring_series.delete')} onClick={() => onDelete(s.id)}
                className="grid size-7 shrink-0 place-items-center rounded-md text-fg-3 hover:bg-status-red-dim hover:text-status-red">
                <Trash2 size={15} />
              </button>
            </li>
          ))}
        </ul>
      )}
    </Dialog>
  )
}
