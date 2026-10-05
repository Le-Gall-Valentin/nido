import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, CTA_BUTTON_STYLE, Input } from '@/shared/ui'
import { isHttpAddress } from '../lib/isHttpAddress'
import { ACTIONS_CLASS, FIELD_ERROR_CLASS, LEAD_CLASS, TITLE_CLASS } from './styles'

interface Props {
  initial: string
  locked: string | null
  serverProblem?: string
  onNext: (url: string) => void
  onBack: () => void
}

export function AddressStep({ initial, locked, serverProblem, onNext, onBack }: Props) {
  const { t } = useTranslation(['setup', 'common'])
  const [url, setUrl] = useState(locked ?? initial)
  const [invalid, setInvalid] = useState(false)
  const shown = locked ?? url

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!locked && !isHttpAddress(url)) {
      setInvalid(true)
      return
    }
    onNext(shown.trim())
  }

  return (
    <form onSubmit={submit} noValidate>
      <h1 className={TITLE_CLASS}>{t('address.title')}</h1>
      <p className={LEAD_CLASS}>{t('address.lead')}</p>
      <Input
        label={t('address.field')}
        name="public-url"
        type="url"
        value={shown}
        disabled={locked !== null}
        onChange={(event) => { setUrl(event.target.value); setInvalid(false) }}
        spellCheck={false}
      />
      {locked && <p className="mt-2 text-[12.5px] text-fg-3">{t('address.locked')}</p>}
      {invalid && <p className={FIELD_ERROR_CLASS}>{t('address.invalid')}</p>}
      {serverProblem && <p className={FIELD_ERROR_CLASS}>{t(`common:setting_problem.${serverProblem}`)}</p>}
      {shown.trim().toLowerCase().startsWith('http://') && (
        <Alert variant="warning" className="mt-4">{t('address.http_warning')}</Alert>
      )}
      <div className={ACTIONS_CLASS}>
        <Button type="button" onClick={onBack}>{t('action.back')}</Button>
        <Button type="submit" className="border-transparent" style={CTA_BUTTON_STYLE}>{t('action.next')}</Button>
      </div>
    </form>
  )
}
