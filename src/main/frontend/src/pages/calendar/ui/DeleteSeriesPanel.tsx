import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import { useDeleteRecurringEventSeries, type RecurringEventSeries } from '@/entities/calendar'
import { runsAcross } from '../lib/seriesInput'

interface DeleteSeriesPanelProps {
  spaceId: string
  series: RecurringEventSeries
  /** The household's today: a series begun before it stops rather than going whole. */
  today: string
  onClose: () => void
}

/**
 * Deleting a whole series. One that has begun and is not over stops the day before today: its past
 * stays, and everything to come goes. Any other goes whole, with its exclusions and its edited
 * occurrences. The confirmation says which, rather than showing the generic warning.
 */
export function DeleteSeriesPanel({ spaceId, series, today, onClose }: DeleteSeriesPanelProps) {
  const { t } = useTranslation('calendar')
  const [error, setError] = useState<string | null>(null)
  const deleteSeries = useDeleteRecurringEventSeries(spaceId)

  return (
    <ConfirmDeleteModal
      title={series.title}
      message={t(runsAcross(series, today) ? 'series.stop_message' : 'series.delete_message')}
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
