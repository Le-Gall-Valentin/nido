import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import { useDeleteRecurringEventSeries, type RecurringEventSeries } from '@/entities/calendar'

interface DeleteSeriesPanelProps {
  spaceId: string
  series: RecurringEventSeries
  onClose: () => void
}

/**
 * Deleting a whole series. It takes its exclusions and its edited occurrences with it, by cascade —
 * which is why the confirmation says so rather than showing the generic warning.
 */
export function DeleteSeriesPanel({ spaceId, series, onClose }: DeleteSeriesPanelProps) {
  const { t } = useTranslation('calendar')
  const [error, setError] = useState<string | null>(null)
  const deleteSeries = useDeleteRecurringEventSeries(spaceId)

  return (
    <ConfirmDeleteModal
      title={series.title}
      message={t('series.delete_message')}
      isPending={deleteSeries.isPending}
      error={error}
      onConfirm={() => {
        setError(null)
        deleteSeries.mutate(series.id, { onSuccess: onClose, onError: () => setError(t('delete.failed')) })
      }}
      onCancel={onClose}
    />
  )
}
