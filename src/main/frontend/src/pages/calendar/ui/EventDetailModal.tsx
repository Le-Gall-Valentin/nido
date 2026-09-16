import { useTranslation } from 'react-i18next'
import { Repeat } from 'lucide-react'
import { Dialog, Button } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import { UserAvatar } from '@/entities/user'
import type { CalendarOccurrence } from '@/entities/calendar'

interface EventDetailModalProps {
  occurrence: CalendarOccurrence
  members: SpaceMember[]
  /** The signed-in user, so "join" and "leave" can be offered as the right one of the two. */
  currentUserId: string
  canWrite: boolean
  /** Copying needs write access at the destination only, so a viewer may still copy. */
  onEdit: () => void
  onDelete: () => void
  onCopy: () => void
  onMove: () => void
  onJoin: () => void
  onLeave: () => void
  onClose: () => void
}

function formatWhen(occurrence: CalendarOccurrence, allDayLabel: string): string {
  const sameDay = occurrence.startDate === occurrence.endDate
  const days = sameDay ? occurrence.startDate : `${occurrence.startDate} → ${occurrence.endDate}`
  if (occurrence.allDay) return `${days} · ${allDayLabel}`
  return `${days} · ${occurrence.startTime?.slice(0, 5)} – ${occurrence.endTime?.slice(0, 5)}`
}

export function EventDetailModal({
  occurrence, members, currentUserId, canWrite,
  onEdit, onDelete, onCopy, onMove, onJoin, onLeave, onClose,
}: EventDetailModalProps) {
  const { t } = useTranslation('calendar')
  const isParticipant = occurrence.participantIds.includes(currentUserId)
  const belongsToSeries = occurrence.seriesId !== null

  return (
    <Dialog open onClose={onClose} title={t('detail.title')} showCloseButton>
      <div className="flex flex-col gap-3">
        <div>
          <p className="flex items-center gap-1.5 pr-8 text-base font-semibold text-fg-0">
            {occurrence.title}
            {belongsToSeries && <Repeat aria-label={t('detail.recurring')} className="size-3.5 shrink-0 text-fg-3" />}
          </p>
          <p className="text-sm text-fg-2">{formatWhen(occurrence, t('all_day_short'))}</p>
        </div>

        <div>
          <h3 className="mb-1 text-xs font-semibold text-fg-2">{t('detail.participants')}</h3>
          {occurrence.participantIds.length === 0 ? (
            <p className="text-sm text-fg-3">{t('detail.no_participants')}</p>
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {occurrence.participantIds.map((userId) => {
                const member = members.find((m) => m.userId === userId)
                return (
                  <UserAvatar key={userId} username={member?.username ?? '?'} role="USER"
                    className="size-7 rounded-full text-[11px]" />
                )
              })}
            </div>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          {/* Copy is offered to everyone: reading an event is enough to justify reproducing it in a
              context where the caller can write. Move and the destructive actions are not. */}
          <Button type="button" onClick={onCopy}>{t('detail.copy')}</Button>
          {canWrite && <Button type="button" onClick={onMove}>{t('detail.move')}</Button>}
          {canWrite && (
            isParticipant
              ? <Button type="button" onClick={onLeave}>{t('detail.leave')}</Button>
              : <Button type="button" onClick={onJoin}>{t('detail.join')}</Button>
          )}
        </div>

        {canWrite && (
          <div className="flex justify-end gap-2 border-t border-border pt-3">
            <Button type="button" onClick={onDelete}>{t('detail.delete')}</Button>
            <Button type="button" onClick={onEdit}>{t('detail.edit')}</Button>
          </div>
        )}
      </div>
    </Dialog>
  )
}
