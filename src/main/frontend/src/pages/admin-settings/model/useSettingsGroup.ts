import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { PASSWORD_RESET_CAPABILITY_KEY } from '@/features/password-reset'
import {
  MailTestFailedError, SettingLockedError, SettingsInvalidError, useSettingWording,
  type InstanceSettings, type SettingField, type SettingsGroup,
} from '@/entities/instance-settings'
import { RateLimitError } from '@/shared/lib'
import { SETTINGS_KEY } from './fields'
import type { ISettingsApi } from './ISettingsApi'

export interface SettingsGroupState {
  values: Record<string, string>
  problems: Record<string, string>
  notice: string | null
  failure: string | null
  busy: boolean
  /** Something in the block is not set by the environment. */
  editable: boolean
  setValue: (key: string, value: string) => void
  save: () => void
  reset: (key: string) => void
  sendTest: () => void
}

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

/** One block of the settings page: what is typed in it, and what saving, resetting or testing it answered. */
export function useSettingsGroup(group: SettingsGroup, api: ISettingsApi): SettingsGroupState {
  const { t } = useTranslation('adminSettings')
  const wording = useSettingWording()
  const queryClient = useQueryClient()
  const [values, setValues] = useState(() => initialValues(group))
  const [problems, setProblems] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => setValues(initialValues(group)), [group])

  const editable = group.fields.some((field) => field.source !== 'ENVIRONMENT')

  function reportFailure(error: unknown) {
    if (error instanceof SettingsInvalidError) setProblems(error.errors)
    else if (error instanceof MailTestFailedError) setFailure(wording.mailFailure(error))
    else if (error instanceof SettingLockedError) setFailure(t('error.locked'))
    else if (error instanceof RateLimitError) setFailure(t('error.too_many'))
    else setFailure(t('error.server'))
  }

  async function attempt(action: () => Promise<string>) {
    setBusy(true)
    setProblems({})
    setNotice(null)
    setFailure(null)
    try {
      setNotice(await action())
    } catch (error) {
      reportFailure(error)
    } finally {
      setBusy(false)
    }
  }

  function apply(change: () => Promise<InstanceSettings>) {
    void attempt(async () => {
      queryClient.setQueryData(SETTINGS_KEY, await change())
      // Mail switched on or off changes what the sign-in page offers.
      void queryClient.invalidateQueries({ queryKey: PASSWORD_RESET_CAPABILITY_KEY })
      return t('saved')
    })
  }

  return {
    values,
    problems,
    notice,
    failure,
    busy,
    editable,
    setValue: (key, value) => setValues((current) => ({ ...current, [key]: value })),
    save: () => {
      if (editable && !busy) apply(() => api.update(group.group, toSend(group.fields, values)))
    },
    reset: (key) => apply(() => api.reset(group.group, key)),
    sendTest: () => void attempt(async () => {
      await api.testMail(toSend(group.fields, values))
      return t('mail.test_sent')
    }),
  }
}
