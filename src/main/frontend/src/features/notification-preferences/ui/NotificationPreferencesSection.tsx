import { Fragment, useId, useState } from 'react'
import { Info } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { NetworkError, RateLimitError } from '@/shared/lib'
import { Alert, Switch } from '@/shared/ui'
import { notificationPreferencesApi } from '../api/notificationPreferencesApi'
import type { INotificationPreferencesApi } from '../model/INotificationPreferencesApi'
import { groupTypes, targetKey } from '../model/preferenceChange'
import type { PreferenceTarget } from '../model/types'
import { useNotificationPreferences } from '../model/useNotificationPreferences'
import { useToggleNotificationPreference } from '../model/useToggleNotificationPreference'

function errorKey(error: unknown): string {
  if (error instanceof RateLimitError) return 'errors.rate_limit'
  if (error instanceof NetworkError) return 'errors.network'
  return 'errors.server'
}

interface RowProps {
  label: string
  description: string
  checked: boolean
  disabled: boolean
  onChange: (checked: boolean) => void
}

function PreferenceRow({ label, description, checked, disabled, onChange }: RowProps) {
  const id = useId()
  return (
    <div className="flex items-start justify-between gap-4">
      <div className="min-w-0">
        <span id={`${id}-label`} className="block text-sm font-semibold text-fg-0">{label}</span>
        <span id={`${id}-description`} className="mt-0.5 block text-[13px] text-fg-2">{description}</span>
      </div>
      <Switch
        checked={checked}
        disabled={disabled}
        onChange={onChange}
        aria-labelledby={`${id}-label`}
        aria-describedby={`${id}-description`}
      />
    </div>
  )
}

function GroupTitle({ children }: { children: React.ReactNode }) {
  return <h4 className="text-[12px] font-semibold uppercase tracking-[0.05em] text-fg-3">{children}</h4>
}

interface Props {
  api?: INotificationPreferencesApi
}

/**
 * The notifications card of the preferences page. Not rendered at all on an installation without a
 * channel (mail not configured): switches for mails that can never come would promise what the server
 * cannot keep. Each switch is saved on its own, at once.
 */
export function NotificationPreferencesSection({ api = notificationPreferencesApi }: Props) {
  const { t } = useTranslation('notificationPreferences')
  const { data, isError } = useNotificationPreferences(api)
  const toggle = useToggleNotificationPreference(api)
  const [pending, setPending] = useState<ReadonlySet<string>>(new Set())
  const [error, setError] = useState<string | null>(null)

  if (!data && !isError) return null
  if (data && data.channels.length === 0) return null

  async function change(target: PreferenceTarget, enabled: boolean) {
    const key = targetKey(target)
    setError(null)
    setPending((current) => new Set(current).add(key))
    try {
      await toggle.mutateAsync({ target, enabled })
    } catch (failure) {
      setError(errorKey(failure))
    } finally {
      setPending((current) => {
        const next = new Set(current)
        next.delete(key)
        return next
      })
    }
  }

  const row = (target: PreferenceTarget, enabled: boolean, labelKey: string) => (
    <PreferenceRow
      key={targetKey(target)}
      label={t(`${labelKey}.label`)}
      description={t(`${labelKey}.description`)}
      checked={enabled}
      disabled={pending.has(targetKey(target))}
      onChange={(next) => void change(target, next)}
    />
  )

  return (
    <section id="section-notifications" className="mb-4 overflow-hidden rounded-2xl border border-border bg-bg-1">
      <div className="px-7 pt-6">
        <h3 className="text-lg font-semibold text-fg-0">{t('title')}</h3>
        <p className="mt-0.5 text-[13.5px] text-fg-2">{t('subtitle')}</p>
      </div>
      <div className="flex flex-col gap-[22px] px-7 py-5">
        {!data ? (
          <Alert variant="error">{t('errors.load')}</Alert>
        ) : (
          <>
            {error && <Alert variant="error">{t(error)}</Alert>}
            <div className="flex flex-col gap-3.5">
              <GroupTitle>{t('channels_title')}</GroupTitle>
              {data.channels.map(({ channel, enabled }) =>
                row({ kind: 'channel', code: channel }, enabled, `channel.${channel}`))}
              {data.channels.every((c) => !c.enabled) && <Alert variant="warning">{t('no_channel')}</Alert>}
            </div>
            {groupTypes(data.types).map(({ group, types }) => (
              <Fragment key={group}>
                <div className="-my-[5px] h-px bg-bg-3" aria-hidden="true" />
                <div className="flex flex-col gap-3.5">
                  <GroupTitle>{t(`group.${group}`)}</GroupTitle>
                  {types.map(({ type, enabled }) => row({ kind: 'type', code: type }, enabled, `type.${type}`))}
                </div>
              </Fragment>
            ))}
            <p className="flex items-start gap-2 text-[12.5px] text-fg-3">
              <Info className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
              {t('security_note')}
            </p>
          </>
        )}
      </div>
    </section>
  )
}
