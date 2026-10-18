import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE, Input, Spinner } from '@/shared/ui'
import { formatRelativeTime } from '@/shared/lib'
import {
  DEFAULT_EVENT_COLOR, eventDotClass, useExternalFeeds, useFollowExternalFeed, useRefreshExternalFeed,
  useRemoveExternalFeed, useUpdateExternalFeed, type EventColor, type ExternalFeed,
} from '@/entities/calendar'
import { FeedColorPicker } from './FeedColorPicker'
import { FeedEditor } from './FeedEditor'
import { feedErrorText, syncErrorMessage } from '../lib/syncWording'

type FocusTarget = 'new' | 'url' | 'rename' | 'confirm' | { edit: string } | { remove: string }

/**
 * The calendars a space follows by address. What an action did is said in one persistent status region, and focus is
 * handed to a neighbour before the element that held it goes away, never left to fall to <body>.
 */
export function ExternalFeedsTab({ spaceId, canWrite }: { spaceId: string; canWrite: boolean }) {
  const { t, i18n } = useTranslation('calendarSync')
  const { data: feeds, isPending } = useExternalFeeds(spaceId)
  const follow = useFollowExternalFeed(spaceId)
  const update = useUpdateExternalFeed(spaceId)
  const remove = useRemoveExternalFeed(spaceId)
  const refresh = useRefreshExternalFeed(spaceId)
  const [adding, setAdding] = useState(false)
  const [url, setUrl] = useState('')
  const [name, setName] = useState('')
  const [color, setColor] = useState<EventColor>(DEFAULT_EVENT_COLOR)
  const [editing, setEditing] = useState<string | null>(null)
  const [removing, setRemoving] = useState<string | null>(null)
  const [message, setMessage] = useState('')
  const pendingFocus = useRef<FocusTarget | null>(null)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const target = pendingFocus.current
    if (!target) return
    pendingFocus.current = null
    const root = rootRef.current
    if (!root) return
    const find = (attribute: string, id?: string) => [...root.querySelectorAll<HTMLElement>(`[${attribute}]`)]
      .find((el) => id === undefined || el.getAttribute(attribute) === id)
    const element = target === 'new' ? find('data-new-feed')
      : target === 'url' ? find('data-feed-url')
      : target === 'rename' ? find('data-rename')
      : target === 'confirm' ? find('data-confirm')
      : 'edit' in target ? find('data-edit-feed', target.edit)
      : find('data-remove-feed', target.remove)
    ;(element ?? root.querySelector<HTMLElement>('[role="status"]'))?.focus()
  })

  /** A new action starts clean: no earlier result or error follows it onto another row or attempt. */
  const resetAll = () => {
    follow.reset()
    update.reset()
    remove.reset()
    refresh.reset()
    setMessage('')
  }

  const status = (feed: ExternalFeed) => feed.lastError
    ? t('feeds.failed_since', { when: formatRelativeTime(feed.contentFetchedAt, i18n.language), reason: feedErrorText(t, feed.lastError) })
    : t('feeds.updated', { when: formatRelativeTime(feed.lastAttemptAt, i18n.language) })

  const leaveForm = () => {
    follow.reset()
    pendingFocus.current = 'new'
    setAdding(false)
  }
  const submit = () => {
    setMessage('')
    follow.mutate({ url: url.trim(), name: name.trim() || null, color }, {
      onSuccess: (followed) => {
        setUrl('')
        setName('')
        pendingFocus.current = 'new'
        setAdding(false)
        setMessage(t('feeds.followed', { name: followed.name }))
      },
    })
  }

  const statusRegion = (
    <p role="status" tabIndex={-1} aria-busy={follow.isPending}
      className={`min-h-5 text-sm text-fg-2 outline-none ${follow.isPending ? 'animate-pulse' : ''}`}>
      {follow.isPending ? t('feeds.saving') : message}
    </p>
  )

  if (adding) {
    return (
      <div ref={rootRef} className="flex flex-col gap-3">
        <Input label={t('feeds.url')} placeholder={t('feeds.url_placeholder')} value={url} inputMode="url" data-feed-url
          onChange={(e) => setUrl(e.target.value)} />
        <Input label={t('feeds.name')} value={name} maxLength={100} onChange={(e) => setName(e.target.value)} />
        <FeedColorPicker value={color} onChange={setColor} />
        {statusRegion}
        {follow.isError && <p role="alert" className="text-sm text-status-red">{syncErrorMessage(t, follow.error)}</p>}
        <div className="flex justify-end gap-2">
          <Button type="button" onClick={leaveForm}>{t('feeds.cancel')}</Button>
          <Button type="button" style={CTA_BUTTON_STYLE} disabled={!url.trim() || follow.isPending} onClick={submit}>
            {t('feeds.save')}
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div ref={rootRef} className="flex flex-col gap-3">
      <p className="text-sm text-fg-2">{t('feeds.intro')}</p>
      {isPending ? (
        <div className="flex justify-center py-4"><Spinner /></div>
      ) : (feeds ?? []).length === 0 ? (
        <p className="text-sm text-fg-3">{t('feeds.empty')}</p>
      ) : (
        <ul className="flex flex-col gap-1">
          {(feeds ?? []).map((feed) => editing === feed.id ? (
            <FeedEditor key={feed.id} feed={feed} pending={update.isPending}
              error={update.isError ? syncErrorMessage(t, update.error) : null}
              onCancel={() => {
                update.reset()
                pendingFocus.current = { edit: feed.id }
                setEditing(null)
              }}
              onSave={(newName, newColor) => update.mutate({ feedId: feed.id, name: newName, color: newColor }, {
                onSuccess: () => {
                  pendingFocus.current = { edit: feed.id }
                  setEditing(null)
                  setMessage(t('feeds.saved', { name: newName }))
                },
              })} />
          ) : (
            <li key={feed.id} className="flex flex-col gap-1 rounded-lg bg-bg-2 px-3 py-2">
              <div className="flex items-start gap-2">
                <span className={`mt-1.5 size-2 shrink-0 rounded-full ${eventDotClass(feed.color)}`} />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm text-fg-1">{feed.name}</p>
                  <p className="text-[11px] text-fg-3">{feed.host} · {status(feed)}</p>
                </div>
              </div>
              {canWrite && removing !== feed.id && (
                <div className="flex flex-wrap justify-end gap-2">
                  <Button type="button" aria-label={t('feeds.refresh_for', { name: feed.name })}
                    aria-disabled={refresh.isPending && refresh.variables === feed.id}
                    onClick={() => {
                      if (refresh.isPending) return
                      resetAll()
                      refresh.mutate(feed.id, { onSuccess: () => setMessage(t('feeds.refreshed', { name: feed.name })) })
                    }}>
                    {t('feeds.refresh')}
                  </Button>
                  <Button type="button" data-edit-feed={feed.id} aria-label={t('feeds.edit_for', { name: feed.name })}
                    onClick={() => {
                      resetAll()
                      pendingFocus.current = 'rename'
                      setEditing(feed.id)
                    }}>
                    {t('feeds.edit')}
                  </Button>
                  <Button type="button" data-remove-feed={feed.id} aria-label={t('feeds.remove_for', { name: feed.name })}
                    onClick={() => {
                      resetAll()
                      pendingFocus.current = 'confirm'
                      setRemoving(feed.id)
                    }}>
                    {t('feeds.remove')}
                  </Button>
                </div>
              )}
              {removing === feed.id && (
                <div data-confirm tabIndex={-1} className="flex flex-wrap items-center justify-end gap-2 outline-none">
                  <p className="flex-1 text-xs text-fg-2">{t('feeds.remove_confirm', { name: feed.name })}</p>
                  <Button type="button" onClick={() => {
                    remove.reset()
                    pendingFocus.current = { remove: feed.id }
                    setRemoving(null)
                  }}>{t('feeds.cancel')}</Button>
                  <Button type="button" style={CTA_BUTTON_STYLE} disabled={remove.isPending}
                    onClick={() => remove.mutate(feed.id, {
                      onSuccess: () => {
                        pendingFocus.current = 'new'
                        setRemoving(null)
                        setMessage(t('feeds.removed', { name: feed.name }))
                      },
                    })}>
                    {t('feeds.remove_yes')}
                  </Button>
                </div>
              )}
              {removing === feed.id && remove.isError && (
                <p role="alert" className="text-sm text-status-red">{syncErrorMessage(t, remove.error)}</p>
              )}
              {refresh.isError && refresh.variables === feed.id && (
                <p role="alert" className="text-sm text-status-red">{syncErrorMessage(t, refresh.error)}</p>
              )}
            </li>
          ))}
        </ul>
      )}
      {statusRegion}
      {canWrite && (
        <div className="flex justify-end">
          <Button type="button" style={CTA_BUTTON_STYLE} data-new-feed onClick={() => {
            resetAll()
            pendingFocus.current = 'url'
            setAdding(true)
          }}>{t('feeds.follow')}</Button>
        </div>
      )}
    </div>
  )
}
