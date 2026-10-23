import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui'
import { formatRelativeTime } from '@/shared/lib'
import type { SubscriptionLink } from '@/entities/calendar'
import { BusyButton } from './BusyButton'

/** One subscription address: what it reads, when it was last read, and its revocation, confirmed in place. */
export function LinkRow({ link, confirming, pending, error, onRevoke, onCancel, onConfirm }: {
  link: SubscriptionLink
  confirming: boolean
  pending: boolean
  error: string | null
  onRevoke: () => void
  onCancel: () => void
  onConfirm: () => void
}) {
  const { t, i18n } = useTranslation('calendarSync')
  return (
    <li className="flex flex-col gap-1 rounded-lg bg-bg-2 px-3 py-2">
      <div className="flex items-center gap-2">
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm text-fg-1">{link.label}</p>
          <p className="text-[11px] text-fg-3">
            {link.sources.map((source) => t(`sources.${source}`)).join(' · ')}
            {' — '}
            {t('subscribe.created_at', { when: formatRelativeTime(link.createdAt, i18n.language) })}
            {' — '}
            {link.lastReadAt
              ? t('subscribe.last_read', { when: formatRelativeTime(link.lastReadAt, i18n.language) })
              : t('subscribe.never_read')}
          </p>
        </div>
        {!confirming && (
          <Button type="button" aria-label={t('subscribe.revoke_for', { label: link.label })} data-revoke-link={link.id}
            onClick={onRevoke}>
            {t('subscribe.revoke')}
          </Button>
        )}
      </div>
      {confirming && (
        <div data-confirm tabIndex={-1} className="flex flex-wrap items-center justify-end gap-2 outline-none">
          <p className="flex-1 text-xs text-fg-2">{t('subscribe.revoke_confirm', { label: link.label })}</p>
          {/* Not while the revocation runs: the address would go all the same, thought kept. */}
          <Button type="button" disabled={pending} onClick={onCancel}>{t('subscribe.cancel')}</Button>
          <BusyButton type="button" variant="primary" busy={pending} onClick={onConfirm}>
            {t('subscribe.revoke_yes')}
          </BusyButton>
        </div>
      )}
      {confirming && error && <p role="alert" className="text-sm text-status-red">{error}</p>}
    </li>
  )
}
