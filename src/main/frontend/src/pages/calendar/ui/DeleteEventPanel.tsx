import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import { useDeleteEvent, useExcludeOccurrence, type CalendarOccurrence } from '@/entities/calendar'

interface DeleteEventPanelProps {
  spaceId: string
  occurrence: CalendarOccurrence
  onClose: () => void
}

/**
 * Deleting an event, or cancelling one occurrence of a series — which are two different calls.
 *
 * A projected occurrence has no row to delete: it is a date computed from a template, so what
 * removes it is an exclusion on its slot. Sending a delete for it would 404, and sending an
 * exclusion for a plain event would be meaningless, so the choice is made from `materialized`
 * rather than from what the reader clicked.
 */
export function DeleteEventPanel({ spaceId, occurrence, onClose }: DeleteEventPanelProps) {
  const { t } = useTranslation('calendar')
  const [error, setError] = useState<string | null>(null)
  const deleteEvent = useDeleteEvent(spaceId)
  const excludeOccurrence = useExcludeOccurrence(spaceId)

  const confirm = () => {
    setError(null)
    const handlers = { onSuccess: onClose, onError: () => setError(t('delete.failed')) }
    if (!occurrence.materialized && occurrence.seriesId && occurrence.originalDate) {
      excludeOccurrence.mutate({ seriesId: occurrence.seriesId, date: occurrence.originalDate }, handlers)
      return
    }
    deleteEvent.mutate(occurrence.sourceId, handlers)
  }

  return (
    <ConfirmDeleteModal
      title={occurrence.title}
      message={occurrence.seriesId ? t('delete.occurrence_message') : undefined}
      isPending={deleteEvent.isPending || excludeOccurrence.isPending}
      error={error}
      onConfirm={confirm}
      onCancel={onClose}
    />
  )
}
