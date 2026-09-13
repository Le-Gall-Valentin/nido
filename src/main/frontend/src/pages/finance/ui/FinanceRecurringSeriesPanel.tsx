import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import {
  useUpdateRecurringSeries, useDeleteRecurringSeries,
  type Category, type RecurringSeries,
} from '@/entities/finance'
import { RecurringSeriesManagerModal } from './RecurringSeriesManagerModal'
import { RecurringSeriesFormModal, type RecurringSeriesFormInput } from './RecurringSeriesFormModal'

interface FinanceRecurringSeriesPanelProps {
  spaceId: string
  /**
   * Passed in rather than queried here: the page loads the series with everything else, so the manager
   * opens on a list that is already there. Asking for them on mount would show an empty list first.
   */
  series: RecurringSeries[]
  categories: Category[]
  members: SpaceMember[]
  canPickContributors: boolean
  onClose: () => void
}

/**
 * Everything that happens once the user opens "manage recurring operations": browse them, edit one,
 * delete one. Two states and two mutations that no other part of the page ever reads, and a flow with
 * one way in and one way out — closing it unmounts the lot.
 */
export function FinanceRecurringSeriesPanel({
  spaceId, series, categories, members, canPickContributors, onClose,
}: FinanceRecurringSeriesPanelProps) {
  const { t } = useTranslation('finance')
  const updateSeries = useUpdateRecurringSeries(spaceId)
  const deleteSeries = useDeleteRecurringSeries(spaceId)

  const [editing, setEditing] = useState<RecurringSeries | null>(null)
  const [deleting, setDeleting] = useState<RecurringSeries | null>(null)

  function handleUpdateSubmit(input: RecurringSeriesFormInput) {
    if (!editing) return
    updateSeries.mutate({ seriesId: editing.id, ...input }, { onSuccess: () => setEditing(null) })
  }

  return (
    <>
      <RecurringSeriesManagerModal
        series={series}
        onEdit={setEditing}
        onDelete={(seriesId) => setDeleting(series.find((s) => s.id === seriesId) ?? null)}
        onClose={onClose}
      />

      {editing && (
        <RecurringSeriesFormModal
          series={editing}
          categories={categories}
          members={members}
          canPickContributors={canPickContributors}
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
          title={t('delete_confirm.title', { label: deleting.label })}
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
