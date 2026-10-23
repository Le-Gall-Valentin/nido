import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE, Spinner } from '@/shared/ui'
import { usePendingFocus } from '@/shared/lib'
import {
  FEED_FALLBACK_COLOR, useExternalFeeds, useFollowExternalFeed, useRefreshExternalFeed, useRemoveExternalFeed,
  useUpdateExternalFeed, type ExternalFeedInput,
} from '@/entities/calendar'
import { BusyButton } from './BusyButton'
import { FeedEditor } from './FeedEditor'
import { FeedRow } from './FeedRow'
import { FollowFeedForm } from './FollowFeedForm'
import { feedErrorText, syncErrorMessage } from '../lib/syncWording'

const NO_FEED: ExternalFeedInput = { url: '', name: null, color: FEED_FALLBACK_COLOR }

/**
 * The calendars a space follows by address. What an action did is said in one persistent status region, and focus is
 * handed to a neighbour before the element that held it goes away, never left to fall to <body>.
 */
export function ExternalFeedsTab({ spaceId, canWrite }: { spaceId: string; canWrite: boolean }) {
  const { t } = useTranslation('calendarSync')
  const { data: feeds, isPending, isError, error, refetch } = useExternalFeeds(spaceId)
  const follow = useFollowExternalFeed(spaceId)
  const update = useUpdateExternalFeed(spaceId)
  const remove = useRemoveExternalFeed(spaceId)
  const refresh = useRefreshExternalFeed(spaceId)
  const [adding, setAdding] = useState(false)
  const [draft, setDraft] = useState<ExternalFeedInput>(NO_FEED)
  const [editing, setEditing] = useState<string | null>(null)
  const [removing, setRemoving] = useState<string | null>(null)
  const [message, setMessage] = useState('')
  const { rootRef, focusNext } = usePendingFocus()
  // One action of a row at a time: a second would race the first, whose outcome would then never be said.
  const busy = update.isPending || remove.isPending || refresh.isPending

  /**
   * A new action starts clean: no earlier result or error follows it onto another row or attempt. A request still
   * running is left alone — reset, it would carry on unheard.
   */
  const resetIdle = () => {
    for (const mutation of [follow, update, remove, refresh]) {
      if (!mutation.isPending) mutation.reset()
    }
    setMessage('')
  }

  const leaveForm = () => {
    follow.reset()
    focusNext('[data-new-feed]')
    setAdding(false)
  }
  const submit = () => {
    setMessage('')
    follow.mutate(draft, {
      onSuccess: (followed) => {
        // The address typed is the mutation's too: reset, it leaves the cache with this tab still open.
        follow.reset()
        focusNext('[data-new-feed]')
        setAdding(false)
        setMessage(t('feeds.followed', { name: followed.name }))
      },
    })
  }

  const list = isPending ? (
    <div className="flex justify-center py-4"><Spinner fullscreen={false} label={t('loading')} /></div>
  ) : isError ? (
    // A list that could not be read is not an empty one: said so, lest a calendar be followed twice.
    <div className="flex flex-wrap items-center gap-2">
      <p role="alert" className="flex-1 text-sm text-status-red">{syncErrorMessage(t, error)}</p>
      <Button type="button" onClick={() => void refetch()}>{t('retry')}</Button>
    </div>
  ) : (feeds ?? []).length === 0 ? (
    <p className="text-sm text-fg-3">{t('feeds.empty')}</p>
  ) : (
    <ul className="flex flex-col gap-1">
      {(feeds ?? []).map((feed) => editing === feed.id ? (
        <FeedEditor key={feed.id} feed={feed} pending={update.isPending}
          error={update.isError ? syncErrorMessage(t, update.error) : null}
          onCancel={() => {
            update.reset()
            focusNext(`[data-edit-feed="${feed.id}"]`)
            setEditing(null)
          }}
          onSave={(newName, newColor) => update.mutate({ feedId: feed.id, name: newName, color: newColor }, {
            onSuccess: () => {
              focusNext(`[data-edit-feed="${feed.id}"]`)
              setEditing(null)
              setMessage(t('feeds.saved', { name: newName }))
            },
          })} />
      ) : (
        <FeedRow key={feed.id} feed={feed} canWrite={canWrite} busy={busy} confirming={removing === feed.id}
          removing={remove.isPending}
          error={removing === feed.id && remove.isError ? syncErrorMessage(t, remove.error)
            : refresh.isError && refresh.variables === feed.id ? syncErrorMessage(t, refresh.error) : null}
          onRefresh={() => {
            resetIdle()
            // A download that failed is recorded and answered 200: the row's error says it, and so does this.
            refresh.mutate(feed.id, {
              onSuccess: (updated) => setMessage(updated.lastError
                ? t('feeds.refresh_failed', { name: feed.name, reason: feedErrorText(t, updated.lastError) })
                : t('feeds.refreshed', { name: feed.name })),
            })
          }}
          onEdit={() => {
            resetIdle()
            focusNext('[data-rename]')
            setEditing(feed.id)
          }}
          onRemove={() => {
            resetIdle()
            focusNext('[data-confirm]')
            setRemoving(feed.id)
          }}
          onCancel={() => {
            remove.reset()
            focusNext(`[data-remove-feed="${feed.id}"]`)
            setRemoving(null)
          }}
          onConfirm={() => remove.mutate(feed.id, {
            onSuccess: () => {
              focusNext('[data-new-feed]')
              setRemoving(null)
              setMessage(t('feeds.removed', { name: feed.name }))
            },
          })} />
      ))}
    </ul>
  )

  return (
    <div ref={rootRef} className="flex flex-col gap-3">
      {adding ? (
        <FollowFeedForm error={follow.isError ? syncErrorMessage(t, follow.error) : null} onChange={setDraft} />
      ) : (
        <>
          <p className="text-sm text-fg-2">{t('feeds.intro')}</p>
          {list}
        </>
      )}
      <p role="status" tabIndex={-1} aria-busy={follow.isPending}
        className={`min-h-5 text-sm text-fg-2 outline-none ${follow.isPending ? 'motion-safe:animate-pulse' : ''}`}>
        {follow.isPending ? t('feeds.saving') : message}
      </p>
      {adding ? (
        <div className="flex justify-end gap-2">
          <Button type="button" disabled={follow.isPending} onClick={leaveForm}>{t('feeds.cancel')}</Button>
          <BusyButton type="button" style={CTA_BUTTON_STYLE} disabled={!draft.url} busy={follow.isPending} onClick={submit}>
            {t('feeds.save')}
          </BusyButton>
        </div>
      ) : canWrite && (
        <div className="flex justify-end">
          <Button type="button" style={CTA_BUTTON_STYLE} data-new-feed onClick={() => {
            resetIdle()
            setDraft(NO_FEED)
            focusNext('[data-feed-url]')
            setAdding(true)
          }}>{t('feeds.follow')}</Button>
        </div>
      )}
    </div>
  )
}
