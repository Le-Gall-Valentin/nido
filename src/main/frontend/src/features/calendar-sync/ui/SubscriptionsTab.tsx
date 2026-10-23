import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Spinner } from '@/shared/ui'
import { usePendingFocus } from '@/shared/lib'
import {
  useCreateSubscriptionLink, useRevokeSubscriptionLink, useSubscriptionLinks, type CreatedSubscriptionLink,
} from '@/entities/calendar'
import { CreatedLinkNotice } from './CreatedLinkNotice'
import { LinkRow } from './LinkRow'
import { NewLinkForm } from './NewLinkForm'
import { syncErrorMessage } from '../lib/syncWording'

/**
 * The secret addresses a calendar app subscribes to. A created address lives in this component's state only:
 * it is dropped by "done", by creating another, by leaving the tab or closing the dialog, and the mutation's own
 * copy is reset as soon as it is handed over. Each view hands focus on to the next one, never to <body>.
 */
export function SubscriptionsTab({ spaceId, isPersonal }: { spaceId: string; isPersonal: boolean }) {
  const { t } = useTranslation('calendarSync')
  const { data: links, isPending, isError, error, refetch } = useSubscriptionLinks(spaceId)
  const create = useCreateSubscriptionLink(spaceId)
  const revoke = useRevokeSubscriptionLink(spaceId)
  const [creating, setCreating] = useState(false)
  const [created, setCreated] = useState<CreatedSubscriptionLink | null>(null)
  const [revoking, setRevoking] = useState<string | null>(null)
  const { rootRef, focusNext } = usePendingFocus()

  const view = created ? (
    <CreatedLinkNotice created={created} onDone={() => {
      focusNext('[data-new-link]')
      setCreated(null)
    }} />
  ) : creating ? (
    <NewLinkForm isPersonal={isPersonal} pending={create.isPending}
      error={create.isError ? syncErrorMessage(t, create.error) : null}
      onCancel={() => {
        create.reset()
        focusNext('[data-new-link]')
        setCreating(false)
      }}
      onCreate={(input) => create.mutate(input, {
        onSuccess: (made) => {
          // The address is shown this once: focus goes to it, so that it is heard and can be copied at once.
          focusNext('[data-created-address]')
          setCreating(false)
          setCreated(made)
          create.reset()
        },
      })} />
  ) : (
    <>
      <p className="text-sm text-fg-2">{t('subscribe.intro')}</p>
      {isPending ? (
        <div className="flex justify-center py-4"><Spinner fullscreen={false} label={t('loading')} /></div>
      ) : isError ? (
        // A list that could not be read is not an empty one: said so, lest an address be made twice.
        <div className="flex flex-wrap items-center gap-2">
          <p role="alert" className="flex-1 text-sm text-status-red">{syncErrorMessage(t, error)}</p>
          <Button type="button" onClick={() => void refetch()}>{t('retry')}</Button>
        </div>
      ) : (links ?? []).length === 0 ? (
        <p className="text-sm text-fg-3">{t('subscribe.empty')}</p>
      ) : (
        <ul className="flex flex-col gap-1">
          {(links ?? []).map((link) => (
            <LinkRow key={link.id} link={link} confirming={revoking === link.id} pending={revoke.isPending}
              error={revoking === link.id && revoke.isError ? syncErrorMessage(t, revoke.error) : null}
              onRevoke={() => {
                revoke.reset()
                focusNext('[data-confirm]')
                setRevoking(link.id)
              }}
              onCancel={() => {
                revoke.reset()
                focusNext(`[data-revoke-link="${link.id}"]`)
                setRevoking(null)
              }}
              onConfirm={() => revoke.mutate(link.id, {
                onSuccess: () => {
                  focusNext('[data-new-link]')
                  setRevoking(null)
                },
              })} />
          ))}
        </ul>
      )}
      <div className="flex justify-end">
        <Button type="button" variant="primary" data-new-link onClick={() => {
          focusNext('[data-link-label]')
          setCreating(true)
        }}>{t('subscribe.new')}</Button>
      </div>
    </>
  )

  return <div ref={rootRef} className="flex flex-col gap-3">{view}</div>
}
