import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui'
import type { GroupExtrasProps } from './types'

/** Mail can be switched off when the page turned it on, and tested always — set by the environment too. */
export function MailActions({ group, state }: GroupExtrasProps) {
  const { t } = useTranslation('adminSettings')
  const host = group.fields.find((field) => field.key === 'mail.host')
  return (
    <>
      {host?.source === 'DATABASE' && (
        <Button type="button" disabled={state.busy} onClick={() => state.reset('mail.host')}>{t('action.disable_mail')}</Button>
      )}
      <Button type="button" disabled={state.busy} onClick={state.sendTest}>{t('action.test')}</Button>
    </>
  )
}
