import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { MailTestFailedError, SettingsInvalidError, useSettingWording } from '@/entities/instance-settings'
import { useLanguage } from '@/shared/lib'
import { Alert, Button, CTA_BUTTON_STYLE, Input, PasswordInput } from '@/shared/ui'
import type { ISetupApi } from '../model/ISetupApi'
import type { SettingValues } from '../model/types'
import { SetupCodeInvalidError } from '../model/errors'
import { describeSetupError } from '../lib/describeSetupError'
import { ACTIONS_CLASS, FIELD_ERROR_CLASS, LEAD_CLASS, TITLE_CLASS } from './styles'

interface Props {
  api: ISetupApi
  code: string
  publicUrl: string
  recipient: string
  locked: boolean
  initial: SettingValues | null
  serverProblems?: Record<string, string>
  onNext: (mail: SettingValues | null) => void
  onBack: () => void
  /** The code was refused: Nido restarted and has another one. */
  onCodeExpired: () => void
}

const DEFAULTS: SettingValues = { 'mail.port': '587', 'mail.security': 'starttls' }
const SECURITIES = ['starttls', 'tls', 'none'] as const

export function MailStep({ api, code, publicUrl, recipient, locked, initial, serverProblems = {}, onNext, onBack, onCodeExpired }: Props) {
  const { t } = useTranslation('setup')
  const wording = useSettingWording()
  const { language } = useLanguage()
  const [values, setValues] = useState<SettingValues>(initial ?? DEFAULTS)
  const [problems, setProblems] = useState<Record<string, string>>(serverProblems)
  const [notice, setNotice] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const [testing, setTesting] = useState(false)

  const set = (key: string) => (value: string) => setValues((current) => ({ ...current, [key]: value }))
  const problem = (key: string) => problems[key] && <p className={FIELD_ERROR_CLASS}>{wording.problem(problems[key])}</p>

  async function test() {
    setTesting(true)
    setProblems({})
    setNotice(null)
    setFailure(null)
    try {
      await api.testMail({ code, mail: values, publicUrl, recipient, language })
      setNotice(t('mail.test_sent', { recipient }))
    } catch (error) {
      if (error instanceof SetupCodeInvalidError) onCodeExpired()
      else if (error instanceof SettingsInvalidError) setProblems(error.errors)
      else if (error instanceof MailTestFailedError) setFailure(wording.mailFailure(error))
      else setFailure(t(describeSetupError(error)))
    } finally {
      setTesting(false)
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!values['mail.host']?.trim()) {
      setProblems({ 'mail.host': 'required' })
      return
    }
    onNext(values)
  }

  if (locked) {
    return (
      <div>
        <h1 className={TITLE_CLASS}>{t('mail.title')}</h1>
        <p className={LEAD_CLASS}>{t('mail.locked')}</p>
        <div className={ACTIONS_CLASS}>
          <Button type="button" onClick={onBack}>{t('action.back')}</Button>
          <Button type="button" className="border-transparent" style={CTA_BUTTON_STYLE} onClick={() => onNext(null)}>{t('action.next')}</Button>
        </div>
      </div>
    )
  }

  return (
    <form onSubmit={submit} noValidate>
      <h1 className={TITLE_CLASS}>{t('mail.title')}</h1>
      <p className={LEAD_CLASS}>{t('mail.lead')}</p>
      <div className="flex flex-col gap-4">
        <div>
          <Input label={t('mail.host')} name="mail-host" value={values['mail.host'] ?? ''} onChange={(e) => set('mail.host')(e.target.value)} spellCheck={false} autoComplete="off" />
          {problem('mail.host')}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <Input label={t('mail.port')} name="mail-port" inputMode="numeric" value={values['mail.port'] ?? ''} onChange={(e) => set('mail.port')(e.target.value)} autoComplete="off" />
            {problem('mail.port')}
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="mail-security" className="text-[13px] font-semibold text-fg-1">{t('mail.security')}</label>
            <select
              id="mail-security"
              value={values['mail.security'] ?? 'starttls'}
              onChange={(e) => set('mail.security')(e.target.value)}
              className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3 py-[11px] text-[14.5px] text-fg-0"
            >
              {SECURITIES.map((security) => <option key={security} value={security}>{t(`mail.security_${security}`)}</option>)}
            </select>
          </div>
        </div>
        <div>
          <Input label={t('mail.username')} name="mail-username" value={values['mail.username'] ?? ''} onChange={(e) => set('mail.username')(e.target.value)} autoComplete="off" />
          {problem('mail.username')}
        </div>
        <div>
          <PasswordInput label={t('mail.password')} name="mail-password" value={values['mail.password'] ?? ''} onChange={(e) => set('mail.password')(e.target.value)} autoComplete="new-password" />
          {problem('mail.password')}
        </div>
        <div>
          <Input label={t('mail.from')} name="mail-from" value={values['mail.from'] ?? ''} placeholder={t('mail.from_placeholder')} onChange={(e) => set('mail.from')(e.target.value)} autoComplete="off" />
          {problem('mail.from')}
          {problem('public-url')}
        </div>
      </div>
      <Button type="button" className="mt-4" isLoading={testing} onClick={() => void test()}>{t('action.test')}</Button>
      {notice && <Alert variant="success" className="mt-4">{notice}</Alert>}
      {failure && <Alert variant="error" className="mt-4">{failure}</Alert>}
      <div className={ACTIONS_CLASS}>
        <Button type="button" onClick={onBack}>{t('action.back')}</Button>
        <div className="flex flex-col-reverse gap-2 sm:flex-row">
          <Button type="button" onClick={() => onNext(null)}>{t('action.later')}</Button>
          <Button type="submit" className="border-transparent" style={CTA_BUTTON_STYLE}>{t('action.next')}</Button>
        </div>
      </div>
    </form>
  )
}
