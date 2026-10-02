import { Fragment, useId, useState, type ReactNode } from 'react'
import { ChevronDown, Info } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { NetworkError, RateLimitError } from '@/shared/lib'
import { Alert, Switch } from '@/shared/ui'
import { notificationPreferencesApi } from '../api/notificationPreferencesApi'
import type { INotificationPreferencesApi } from '../model/INotificationPreferencesApi'
import { groupTypes, targetKey } from '../model/preferenceChange'
import type { PreferenceTarget } from '../model/types'
import { useNotificationPreferences } from '../model/useNotificationPreferences'
import { useToggleNotificationPreference } from '../model/useToggleNotificationPreference'
import { useCollapsedGroups } from '../model/useCollapsedGroups'

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

function GroupTitle({ children }: { children: ReactNode }) {
  return <h4 className="text-[12px] font-semibold uppercase tracking-[0.05em] text-fg-3">{children}</h4>
}

interface TypeGroupProps {
  title: string
  summary: string
  collapsed: boolean
  onToggle: () => void
  children: ReactNode
}

/** A category of kinds, folded or not; folded, it says how many of its kinds are on. */
function TypeGroup({ title, summary, collapsed, onToggle, children }: TypeGroupProps) {
  const panelId = useId()
  return (
    <div className="flex flex-col gap-3.5">
      <h4>
        <button
          type="button"
          aria-expanded={!collapsed}
          aria-controls={panelId}
          onClick={onToggle}
          className="flex w-full items-center justify-between gap-3 text-left"
        >
          <span className="text-[12px] font-semibold uppercase tracking-[0.05em] text-fg-3">{title}</span>
          <span className="flex items-center gap-1.5 text-[12.5px] text-fg-3">
            {collapsed && <span>{summary}</span>}
            <ChevronDown aria-hidden="true" className={`size-4 transition-transform ${collapsed ? '-rotate-90' : ''}`} />
          </span>
        </button>
      </h4>
      <div id={panelId} hidden={collapsed} className="flex flex-col gap-3.5">
        {children}
      </div>
    </div>
  )
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
  const groups = useCollapsedGroups()
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
                <TypeGroup
                  title={t(`group.${group}`)}
                  summary={t('group_summary', { count: types.filter((type) => type.enabled).length, total: types.length })}
                  collapsed={groups.isCollapsed(group)}
                  onToggle={() => groups.toggle(group)}
                >
                  {types.map(({ type, enabled }) => row({ kind: 'type', code: type }, enabled, `type.${type}`))}
                </TypeGroup>
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
