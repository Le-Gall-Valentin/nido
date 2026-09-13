import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import {
  useRecurringTaskSeries, useUpdateRecurringTaskSeries, useDeleteRecurringTaskSeries,
  type RecurringTaskSeries,
} from '@/entities/tasks'
import type { RecurringTaskSeriesFormInput } from '../model/types'
import { RecurringTaskSeriesManagerModal } from './RecurringTaskSeriesManagerModal'
import { RecurringTaskSeriesFormModal } from './RecurringTaskSeriesFormModal'
import { RecurringTaskSeriesDetailModal } from './RecurringTaskSeriesDetailModal'

interface RecurringSeriesPanelProps {
  spaceId: string
  members: SpaceMember[]
  isPersonal: boolean
  onClose: () => void
}

/**
 * Everything that happens once the user opens "manage recurring series": browse them, look at one,
 * edit one, delete one.
 *
 * <p>It exists as a component rather than as four more slices of the page's state because that is what
 * the four steps are — one flow, with one way in and one way out. Held in the page, they were four
 * {@code useState} and two mutations that nothing else on the board ever touched, and the page had to
 * know the order they happen in. Here the state cannot outlive the flow: closing the panel unmounts
 * it, so there is no "editing a series that is no longer on screen" to guard against.
 *
 * <p>The page keeps exactly one bit — whether this is open — because that is the part the header
 * button decides.
 */
export function RecurringSeriesPanel({ spaceId, members, isPersonal, onClose }: RecurringSeriesPanelProps) {
  const { t } = useTranslation('tasks')
  const { data: series } = useRecurringTaskSeries(spaceId)
  const updateSeries = useUpdateRecurringTaskSeries(spaceId)
  const deleteSeries = useDeleteRecurringTaskSeries(spaceId)

  const [viewing, setViewing] = useState<RecurringTaskSeries | null>(null)
  const [editing, setEditing] = useState<RecurringTaskSeries | null>(null)
  const [deleting, setDeleting] = useState<RecurringTaskSeries | null>(null)

  function handleUpdateSubmit(input: RecurringTaskSeriesFormInput) {
    if (!editing) return
    updateSeries.mutate({ seriesId: editing.id, ...input }, { onSuccess: () => setEditing(null) })
  }

  return (
    <>
      <RecurringTaskSeriesManagerModal
        series={series ?? []}
        onView={setViewing}
        onEdit={setEditing}
        onDelete={(seriesId) => setDeleting((series ?? []).find((s) => s.id === seriesId) ?? null)}
        onClose={onClose}
      />

      {viewing && (
        <RecurringTaskSeriesDetailModal
          series={viewing}
          members={members}
          onClose={() => setViewing(null)}
        />
      )}

      {editing && (
        <RecurringTaskSeriesFormModal
          series={editing}
          members={members}
          isPersonal={isPersonal}
          onSubmit={handleUpdateSubmit}
          onCancel={() => {
            setEditing(null)
            updateSeries.reset()
          }}
          submitError={updateSeries.isError ? t('form.submit_error') : null}
        />
      )}

      {deleting && (
        <ConfirmDeleteModal
          title={t('delete_confirm.title', { title: deleting.title })}
          message={t('delete_confirm.message')}
          confirmLabel={t('delete_confirm.confirm')}
          cancelLabel={t('delete_confirm.cancel')}
          isPending={deleteSeries.isPending}
          error={deleteSeries.isError ? t('delete_confirm.error') : null}
          onCancel={() => {
            setDeleting(null)
            deleteSeries.reset()
          }}
          onConfirm={() => deleteSeries.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      )}
    </>
  )
}
