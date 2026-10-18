import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE, Input, Spinner } from '@/shared/ui'
import { formatRelativeTime } from '@/shared/lib'
import {
  useCreateSubscriptionLink, useRevokeSubscriptionLink, useSubscriptionLinks,
  type CreatedSubscriptionLink, type ExportableSource, type SubscriptionLink,
} from '@/entities/calendar'
import { OnlyMineToggle, SourceChecklist } from './SourceChecklist'
import { CreatedLinkNotice } from './CreatedLinkNotice'
import { syncErrorMessage } from '../lib/syncWording'

/**
 * The secret addresses a calendar app subscribes to. A created address lives in this component's state only:
 * it is dropped by "done", by creating another, by leaving the tab or closing the dialog, and the mutation's own
 * copy is reset as soon as it is handed over.
 */
export function SubscriptionsTab({ spaceId, isPersonal }: { spaceId: string; isPersonal: boolean }) {
  const { t, i18n } = useTranslation('calendarSync')
  const { data: links, isPending } = useSubscriptionLinks(spaceId)
  const create = useCreateSubscriptionLink(spaceId)
  const revoke = useRevokeSubscriptionLink(spaceId)
  const [creating, setCreating] = useState(false)
  const [created, setCreated] = useState<CreatedSubscriptionLink | null>(null)
  const [revoking, setRevoking] = useState<string | null>(null)
  const [label, setLabel] = useState('')
  const [sources, setSources] = useState<ExportableSource[]>(['EVENT'])
  const [onlyMine, setOnlyMine] = useState(false)
  // Where focus goes after the next render: a button unmounts when its panel changes, and focus would fall to <body>.
  const pendingFocus = useRef<'confirm' | 'new' | { revoke: string } | null>(null)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const target = pendingFocus.current
    if (!target) return
    pendingFocus.current = null
    const root = rootRef.current
    if (!root) return
    const element = target === 'confirm' ? root.querySelector<HTMLElement>('[data-confirm]')
      : target === 'new' ? root.querySelector<HTMLElement>('[data-new-link]')
      : [...root.querySelectorAll<HTMLElement>('[data-revoke-link]')].find((el) => el.dataset.revokeLink === target.revoke)
    element?.focus()
  })

  if (created) {
    return (
      <div ref={rootRef}>
        <CreatedLinkNotice created={created} onDone={() => {
          pendingFocus.current = 'new'
          setCreated(null)
        }} />
      </div>
    )
  }

  if (creating) {
    const submit = () => create.mutate({ label: label.trim(), sources, onlyMine }, {
      onSuccess: (made) => {
        setCreating(false)
        setLabel('')
        setCreated(made)
        create.reset()
      },
    })
    const cancel = () => {
      create.reset()
      pendingFocus.current = 'new'
      setCreating(false)
    }
    return (
      <div className="flex flex-col gap-3">
        <Input label={t('subscribe.label')} placeholder={t('subscribe.label_placeholder')} value={label} maxLength={60}
          onChange={(e) => setLabel(e.target.value)} />
        <SourceChecklist selected={sources} onChange={setSources} />
        {!isPersonal && <OnlyMineToggle checked={onlyMine} onChange={setOnlyMine} />}
        {create.isError && <p role="alert" className="text-sm text-status-red">{syncErrorMessage(t, create.error)}</p>}
        <div className="flex justify-end gap-2">
          <Button type="button" onClick={cancel}>{t('subscribe.cancel')}</Button>
          <Button type="button" style={CTA_BUTTON_STYLE} onClick={submit}
            disabled={!label.trim() || sources.length === 0 || create.isPending}>
            {t('subscribe.create')}
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div ref={rootRef} className="flex flex-col gap-3">
      <p className="text-sm text-fg-2">{t('subscribe.intro')}</p>
      {isPending ? (
        <div className="flex justify-center py-4"><Spinner /></div>
      ) : (links ?? []).length === 0 ? (
        <p className="text-sm text-fg-3">{t('subscribe.empty')}</p>
      ) : (
        <ul className="flex flex-col gap-1">
          {(links ?? []).map((link: SubscriptionLink) => (
            <li key={link.id} className="flex flex-col gap-1 rounded-lg bg-bg-2 px-3 py-2">
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
                {revoking !== link.id && (
                  <Button type="button" aria-label={t('subscribe.revoke_for', { label: link.label })}
                    data-revoke-link={link.id}
                    onClick={() => {
                      revoke.reset()
                      pendingFocus.current = 'confirm'
                      setRevoking(link.id)
                    }}>
                    {t('subscribe.revoke')}
                  </Button>
                )}
              </div>
              {revoking === link.id && (
                <div data-confirm tabIndex={-1} className="flex flex-wrap items-center justify-end gap-2 outline-none">
                  <p className="flex-1 text-xs text-fg-2">{t('subscribe.revoke_confirm', { label: link.label })}</p>
                  <Button type="button" onClick={() => {
                    revoke.reset()
                    pendingFocus.current = { revoke: link.id }
                    setRevoking(null)
                  }}>{t('subscribe.cancel')}</Button>
                  <Button type="button" style={CTA_BUTTON_STYLE} disabled={revoke.isPending}
                    onClick={() => revoke.mutate(link.id, {
                      onSuccess: () => {
                        pendingFocus.current = 'new'
                        setRevoking(null)
                      },
                    })}>
                    {t('subscribe.revoke_yes')}
                  </Button>
                </div>
              )}
              {revoking === link.id && revoke.isError && (
                <p role="alert" className="text-sm text-status-red">{syncErrorMessage(t, revoke.error)}</p>
              )}
            </li>
          ))}
        </ul>
      )}
      <div className="flex justify-end">
        <Button type="button" style={CTA_BUTTON_STYLE} data-new-link onClick={() => setCreating(true)}>{t('subscribe.new')}</Button>
      </div>
    </div>
  )
}
