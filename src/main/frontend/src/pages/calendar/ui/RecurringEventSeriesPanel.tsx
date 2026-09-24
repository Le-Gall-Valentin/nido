import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { ConfirmDeleteModal, Dialog, Button, Spinner, CTA_BUTTON_STYLE } from '@/shared/ui'
import {
  useRecurringEventSeries, useCreateRecurringEventSeries, useUpdateRecurringEventSeries,
  useDeleteRecurringEventSeries, type RecurringEventSeries, type RecurringEventSeriesInput,
} from '@/entities/calendar'
import type { SpaceMember } from '@/entities/space'
import { RecurringEventSeriesFormModal } from './RecurringEventSeriesFormModal'

interface RecurringEventSeriesPanelProps {
  spaceId: string
  members: SpaceMember[]
  currentUserId: string
  isPersonal: boolean
  onClose: () => void
}

/**
 * Managing the templates every recurring occurrence comes from.
 *
 * Deleting one takes its exclusions and its edited occurrences with it, by cascade — which is why
 * the confirmation says so rather than showing the generic warning.
 */
export function RecurringEventSeriesPanel({
  spaceId, members, currentUserId, isPersonal, onClose,
}: RecurringEventSeriesPanelProps) {
  const { t } = useTranslation('calendar')
  const { data: series, isPending } = useRecurringEventSeries(spaceId)
  const createSeries = useCreateRecurringEventSeries(spaceId)
  const updateSeries = useUpdateRecurringEventSeries(spaceId)
  const deleteSeries = useDeleteRecurringEventSeries(spaceId)

  const [editing, setEditing] = useState<{ series: RecurringEventSeries | null } | null>(null)
  const [deleting, setDeleting] = useState<RecurringEventSeries | null>(null)
  const [error, setError] = useState<string | null>(null)

  const submit = (input: RecurringEventSeriesInput) => {
    setError(null)
    const handlers = { onSuccess: () => setEditing(null), onError: () => setError(t('form.save_failed')) }
    if (editing?.series) updateSeries.mutate({ seriesId: editing.series.id, input }, handlers)
    else createSeries.mutate(input, handlers)
  }

  if (editing) {
    return (
      <RecurringEventSeriesFormModal
        series={editing.series}
        members={members}
        currentUserId={currentUserId}
        isPersonal={isPersonal}
        submitError={error}
        isPending={createSeries.isPending || updateSeries.isPending}
        onSubmit={submit}
        onCancel={() => { setEditing(null); setError(null) }}
      />
    )
  }

  if (deleting) {
    return (
      <ConfirmDeleteModal
        title={deleting.title}
        message={t('series.delete_message')}
        isPending={deleteSeries.isPending}
        onConfirm={() => deleteSeries.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        onCancel={() => setDeleting(null)}
      />
    )
  }

  return (
    <Dialog open onClose={onClose} title={t('recurring_series.manage')} showCloseButton>
      {isPending ? (
        <div className="flex justify-center py-6"><Spinner /></div>
      ) : (
        <div className="flex flex-col gap-3">
          {(series ?? []).length === 0 ? (
            <p className="text-sm text-fg-3">{t('series.empty')}</p>
          ) : (
            <ul className="flex flex-col gap-1">
              {(series ?? []).map((candidate) => (
                <li key={candidate.id} className="flex items-center gap-2 rounded-lg bg-bg-2 px-2 py-1.5">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm text-fg-1">{candidate.title}</p>
                    <p className="text-[11px] text-fg-3">
                      {t(`series.interval.${candidate.intervalType}`)} · {t('series.from', { date: candidate.anchorDate })}
                    </p>
                  </div>
                  <button type="button" aria-label={t('series.edit', { name: candidate.title })}
                    onClick={() => setEditing({ series: candidate })}
                    className="grid size-7 place-items-center rounded-md text-fg-3 hover:text-fg-1">
                    <Pencil className="size-4" />
                  </button>
                  <button type="button" aria-label={t('series.delete', { name: candidate.title })}
                    onClick={() => setDeleting(candidate)}
                    className="grid size-7 place-items-center rounded-md text-fg-3 hover:text-status-red">
                    <Trash2 className="size-4" />
                  </button>
                </li>
              ))}
            </ul>
          )}
          <div className="flex justify-end">
            <Button type="button" style={CTA_BUTTON_STYLE} onClick={() => setEditing({ series: null })}>
              <Plus className="size-4" /> {t('series.new')}
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  )
}
