import { useEffect, useId, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { PASSWORD_RESET_CAPABILITY_KEY } from '@/features/password-reset'
import { MailTestFailedError, RateLimitError, SettingLockedError, SettingsInvalidError } from '@/shared/lib'
import { Alert, Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { SETTINGS_KEY } from '../model/fields'
import type { ISettingsApi } from '../model/ISettingsApi'
import type { InstanceSettings, SettingField, SettingsGroup } from '../model/types'
import { SettingFieldRow } from './SettingFieldRow'

function initialValues(group: SettingsGroup): Record<string, string> {
  return Object.fromEntries(group.fields.map((field) => [field.key, field.secret ? '' : field.value ?? '']))
}

/** What a save sends: every field the page may change — a password only when one was typed. */
function toSend(fields: SettingField[], values: Record<string, string>): Record<string, string> {
  return Object.fromEntries(fields
    .filter((field) => field.source !== 'ENVIRONMENT')
    .filter((field) => !field.secret || values[field.key] !== '')
    .map((field) => [field.key, values[field.key] ?? '']))
}

export function SettingsGroupCard({ group, api }: { group: SettingsGroup; api: ISettingsApi }) {
  const { t } = useTranslation(['adminSettings', 'common'])
  const titleId = useId()
  const queryClient = useQueryClient()
  const [values, setValues] = useState(() => initialValues(group))
  const [problems, setProblems] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => setValues(initialValues(group)), [group])

  const editable = group.fields.some((field) => field.source !== 'ENVIRONMENT')
  const mailHost = group.fields.find((field) => field.key === 'mail.host')

  function reportFailure(error: unknown) {
    if (error instanceof SettingsInvalidError) setProblems(error.errors)
    else if (error instanceof MailTestFailedError) {
      setFailure([t(`common:mail_failure.${error.reason}`), error.serverReply].filter(Boolean).join(' — '))
    }
    else if (error instanceof SettingLockedError) setFailure(t('error.locked'))
    else if (error instanceof RateLimitError) setFailure(t('error.too_many'))
    else setFailure(t('error.server'))
  }

  async function run(action: () => Promise<InstanceSettings>) {
    setBusy(true)
    setProblems({})
    setNotice(null)
    setFailure(null)
    try {
      queryClient.setQueryData(SETTINGS_KEY, await action())
      // Mail switched on or off changes what the sign-in page offers.
      void queryClient.invalidateQueries({ queryKey: PASSWORD_RESET_CAPABILITY_KEY })
      setNotice(t('saved'))
    } catch (error) {
      reportFailure(error)
    } finally {
      setBusy(false)
    }
  }

  async function sendTest() {
    setBusy(true)
    setProblems({})
    setNotice(null)
    setFailure(null)
    try {
      await api.testMail(toSend(group.fields, values))
      setNotice(t('mail.test_sent'))
    } catch (error) {
      reportFailure(error)
    } finally {
      setBusy(false)
    }
  }

  const publicUrl = values['public-url'] ?? ''

  return (
    <section aria-labelledby={titleId} className="rounded-2xl border border-border bg-bg-1 p-5 sm:p-6">
      <h2 id={titleId} className="text-[17px] font-semibold text-fg-0">{t(`group.${group.group}.title`)}</h2>
      <p className="mt-1 text-[13.5px] leading-relaxed text-fg-2">{t(`group.${group.group}.description`)}</p>

      <div className="mt-5 flex flex-col gap-4">
        {group.fields.map((field) => (
          <SettingFieldRow
            key={field.key}
            field={field}
            value={values[field.key] ?? ''}
            problem={problems[field.key]}
            busy={busy}
            onChange={(value) => setValues((current) => ({ ...current, [field.key]: value }))}
            onReset={() => void run(() => api.reset(group.group, field.key))}
          />
        ))}
      </div>

      {group.group === 'public-url' && publicUrl.toLowerCase().startsWith('http://') && (
        <Alert variant="warning" className="mt-4">{t('public_url.http_warning')}</Alert>
      )}
      {group.group === 'public-url' && publicUrl.toLowerCase().startsWith('https://') && window.location.protocol === 'http:' && (
        <Alert variant="warning" className="mt-4">{t('public_url.https_from_http', { url: publicUrl })}</Alert>
      )}
      {group.group === 'mail' && problems['public-url'] && (
        <p className="mt-3 text-[12.5px] text-status-red">{t(`common:setting_problem.${problems['public-url']}`)}</p>
      )}

      {notice && <Alert variant="success" className="mt-4">{notice}</Alert>}
      {failure && <Alert variant="error" className="mt-4">{failure}</Alert>}

      {editable && (
        <div className="mt-5 flex flex-wrap items-center justify-end gap-2">
          {group.group === 'mail' && mailHost?.source === 'DATABASE' && (
            <Button type="button" disabled={busy} onClick={() => void run(() => api.reset('mail', 'mail.host'))}>{t('action.disable_mail')}</Button>
          )}
          {group.group === 'mail' && (
            <Button type="button" disabled={busy} onClick={() => void sendTest()}>{t('action.test')}</Button>
          )}
          <Button
            type="button"
            className="border-transparent"
            style={CTA_BUTTON_STYLE}
            isLoading={busy}
            onClick={() => void run(() => api.update(group.group, toSend(group.fields, values)))}
          >
            {t('action.save')}
          </Button>
        </div>
      )}
    </section>
  )
}
