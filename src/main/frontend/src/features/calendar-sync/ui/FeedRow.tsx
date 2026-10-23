import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui'
import { formatRelativeTime } from '@/shared/lib'
import { EVENT_DOT_CLASS, feedColor, type ExternalFeed } from '@/entities/calendar'
import { BusyButton } from './BusyButton'
import { feedErrorText } from '../lib/syncWording'

/**
 * One followed calendar: where it comes from, how its last asking went, and what a member may do with it — refresh,
 * edit, remove (confirmed in place). While any action of the tab runs, the others wait: none is started twice, and
 * the outcome of each is said.
 */
export function FeedRow({ feed, canWrite, busy, confirming, removing, error, onRefresh, onEdit, onRemove, onCancel, onConfirm }: {
  feed: ExternalFeed
  canWrite: boolean
  busy: boolean
  confirming: boolean
  removing: boolean
  error: string | null
  onRefresh: () => void
  onEdit: () => void
  onRemove: () => void
  onCancel: () => void
  onConfirm: () => void
}) {
  const { t, i18n } = useTranslation('calendarSync')
  const status = feed.lastError
    ? t('feeds.failed_since', { when: formatRelativeTime(feed.contentFetchedAt, i18n.language), reason: feedErrorText(t, feed.lastError) })
    : t('feeds.updated', { when: formatRelativeTime(feed.lastAttemptAt, i18n.language) })
  return (
    <li className="flex flex-col gap-1 rounded-lg bg-bg-2 px-3 py-2">
      <div className="flex items-start gap-2">
        <span aria-hidden className={`mt-1.5 size-2 shrink-0 rounded-full ${EVENT_DOT_CLASS[feedColor(feed.color)]}`} />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm text-fg-1">{feed.name}</p>
          <p className="text-[11px] text-fg-3">{feed.host} · {status}</p>
        </div>
      </div>
      {canWrite && !confirming && (
        <div className="flex flex-wrap justify-end gap-2">
          <BusyButton type="button" aria-label={t('feeds.refresh_for', { name: feed.name })} busy={busy}
            onClick={onRefresh}>
            {t('feeds.refresh')}
          </BusyButton>
          <BusyButton type="button" data-edit-feed={feed.id} aria-label={t('feeds.edit_for', { name: feed.name })}
            busy={busy} onClick={onEdit}>
            {t('feeds.edit')}
          </BusyButton>
          <BusyButton type="button" data-remove-feed={feed.id} aria-label={t('feeds.remove_for', { name: feed.name })}
            busy={busy} onClick={onRemove}>
            {t('feeds.remove')}
          </BusyButton>
        </div>
      )}
      {confirming && (
        <div data-confirm tabIndex={-1} className="flex flex-wrap items-center justify-end gap-2 outline-none">
          <p className="flex-1 text-xs text-fg-2">{t('feeds.remove_confirm', { name: feed.name })}</p>
          {/* Not while the removal runs: the calendar would go all the same, thought kept. */}
          <Button type="button" disabled={removing} onClick={onCancel}>{t('feeds.cancel')}</Button>
          <BusyButton type="button" variant="primary" busy={removing} onClick={onConfirm}>
            {t('feeds.remove_yes')}
          </BusyButton>
        </div>
      )}
      {error && <p role="alert" className="text-sm text-status-red">{error}</p>}
    </li>
  )
}
