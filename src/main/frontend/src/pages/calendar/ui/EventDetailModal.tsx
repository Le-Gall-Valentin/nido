import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { MapPin, Repeat } from 'lucide-react'
import { Dialog, Button } from '@/shared/ui'
import { resolveLocale } from '@/shared/lib'
import type { SpaceMember } from '@/entities/space'
import { UserAvatar } from '@/entities/user'
import type { CalendarOccurrence } from '@/entities/calendar'
import { formatPeriodLabel, formatSpan } from '../lib/periodLabel'

interface EventDetailModalProps {
  occurrence: CalendarOccurrence
  members: SpaceMember[]
  /** The signed-in user, so "join" and "leave" can be offered as the right one of the two. */
  currentUserId: string
  canWrite: boolean
  /** A personal context: its owner always takes part, so there is no one to show, join or leave. */
  isPersonal?: boolean
  /** Copying needs write access at the destination only, so a viewer may still copy. */
  onEdit: () => void
  onDelete: () => void
  onCopy: () => void
  onMove: () => void
  onJoin: () => void
  onLeave: () => void
  onClose: () => void
}

/** The same wording as the calendar's own headings: a day in full, a span shortened only as it can be. */
function formatWhen(occurrence: CalendarOccurrence, allDayLabel: string, locale: string): string {
  const days = occurrence.startDate === occurrence.endDate
    ? formatPeriodLabel('day', occurrence.startDate, locale)
    : formatSpan(occurrence.startDate, occurrence.endDate, locale)
  if (occurrence.allDay) return `${days} · ${allDayLabel}`
  return `${days} · ${occurrence.startTime?.slice(0, 5)} – ${occurrence.endTime?.slice(0, 5)}`
}

/**
 * A button of the actions row: an equal share of it. The 9rem start is wider than the longest label,
 * so no label ever makes its button wider than the others; the modal is as wide as the event form,
 * which leaves room for three of them on a line.
 */
const SHARED_ROW = 'flex-[1_1_9rem] whitespace-nowrap'

export function EventDetailModal({
  occurrence, members, currentUserId, canWrite, isPersonal = false,
  onEdit, onDelete, onCopy, onMove, onJoin, onLeave, onClose,
}: EventDetailModalProps) {
  const { t } = useTranslation('calendar')
  const isParticipant = occurrence.participantIds.includes(currentUserId)
  const belongsToSeries = occurrence.seriesId !== null

  return (
    <Dialog open onClose={onClose} title={t('detail.title')} showCloseButton maxWidth="max-w-lg">
      <div className="flex flex-col gap-3">
        <div>
          <p className="flex items-center gap-1.5 pr-8 text-base font-semibold text-fg-0">
            {occurrence.title}
            {belongsToSeries && <Repeat aria-label={t('detail.recurring')} className="size-3.5 shrink-0 text-fg-3" />}
          </p>
          <p className="text-sm text-fg-2">
            {formatWhen(occurrence, t('all_day_short'), resolveLocale(i18next.language))}
          </p>
        </div>

        {occurrence.location && (
          <p data-testid="event-location" className="flex items-center gap-1.5 text-sm text-fg-1">
            <MapPin aria-hidden className="size-4 shrink-0 text-fg-3" />
            {occurrence.location}
          </p>
        )}

        {occurrence.description && (
          // In full here, unlike the day overview: this is where the reader came to read it.
          <p data-testid="event-description" className="whitespace-pre-line text-sm text-fg-1">
            {occurrence.description}
          </p>
        )}

        {!isPersonal && (
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
        )}

        {/* Equal shares of the whole row. A button that no longer fits at its 9rem goes to the next
            line and takes all of it — which is where "leave" lands on a phone. */}
        <div className="flex flex-wrap gap-2">
          {/* Copy is offered to everyone: reading an event is enough to justify reproducing it in a
              context where the caller can write. Move and the destructive actions are not. */}
          <Button type="button" onClick={onCopy} className={SHARED_ROW}>{t('detail.copy')}</Button>
          {canWrite && <Button type="button" onClick={onMove} className={SHARED_ROW}>{t('detail.move')}</Button>}
          {canWrite && !isPersonal && (
            isParticipant
              ? <Button type="button" onClick={onLeave} className={SHARED_ROW}>{t('detail.leave')}</Button>
              : <Button type="button" onClick={onJoin} className={SHARED_ROW}>{t('detail.join')}</Button>
          )}
        </div>

        {canWrite && (
          <div className="flex gap-2 border-t border-border pt-3">
            <Button type="button" onClick={onEdit} className="flex-1">{t('detail.edit')}</Button>
            <Button type="button" onClick={onDelete} className="flex-1">{t('detail.delete')}</Button>
          </div>
        )}
      </div>
    </Dialog>
  )
}
