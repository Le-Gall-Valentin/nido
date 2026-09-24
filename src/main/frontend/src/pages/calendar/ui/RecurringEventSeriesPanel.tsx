import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Pencil, Trash2 } from 'lucide-react'
import { Dialog, Spinner } from '@/shared/ui'
import { useRecurringEventSeries, type RecurringEventSeries } from '@/entities/calendar'
import type { SpaceMember } from '@/entities/space'
import { EventFormPanel } from './EventFormPanel'
import { DeleteSeriesPanel } from './DeleteSeriesPanel'

interface RecurringEventSeriesPanelProps {
  spaceId: string
  members: SpaceMember[]
  currentUserId: string
  isPersonal: boolean
  onClose: () => void
}

/**
 * The recurring events of a space, to find one again and edit or delete it. A series is created
 * from the new-event form, by ticking "recurring" — and edited in that same form.
 */
export function RecurringEventSeriesPanel({
  spaceId, members, currentUserId, isPersonal, onClose,
}: RecurringEventSeriesPanelProps) {
  const { t } = useTranslation('calendar')
  const { data: series, isPending } = useRecurringEventSeries(spaceId)

  const [editing, setEditing] = useState<RecurringEventSeries | null>(null)
  const [deleting, setDeleting] = useState<RecurringEventSeries | null>(null)

  if (editing) {
    return (
      <EventFormPanel spaceId={spaceId} occurrence={null} series={editing} detachSlot={null}
        defaultDate={editing.anchorDate} currentUserId={currentUserId} members={members} isPersonal={isPersonal}
        onClose={() => setEditing(null)} />
    )
  }

  if (deleting) {
    return <DeleteSeriesPanel spaceId={spaceId} series={deleting} onClose={() => setDeleting(null)} />
  }

  return (
    <Dialog open onClose={onClose} title={t('recurring_series.manage')} showCloseButton>
      {isPending ? (
        <div className="flex justify-center py-6"><Spinner /></div>
      ) : (series ?? []).length === 0 ? (
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
                onClick={() => setEditing(candidate)}
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
    </Dialog>
  )
}
