import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, CTA_BUTTON_STYLE, Input } from '@/shared/ui'
import type { ISetupApi } from '../model/ISetupApi'
import { describeSetupError } from '../lib/describeSetupError'
import { LEAD_CLASS, PRIMARY_CLASS, TITLE_CLASS } from './styles'

interface Props {
  api: ISetupApi
  /** Why the code is asked again, when it is (a translation key). */
  notice?: string | null
  onVerified: (code: string) => void
}

export function CodeStep({ api, notice = null, onVerified }: Props) {
  const { t } = useTranslation('setup')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    const typed = code.trim()
    if (!typed) return
    setBusy(true)
    setError(null)
    try {
      await api.verifyCode(typed)
      onVerified(typed)
    } catch (failure) {
      setError(describeSetupError(failure))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={(event) => void submit(event)} noValidate>
      <h1 className={TITLE_CLASS}>{t('code.title')}</h1>
      {notice && <Alert variant="warning" className="mb-4">{t(notice)}</Alert>}
      <p className={LEAD_CLASS}>{t('code.lead')}</p>
      <pre className="mb-5 overflow-x-auto rounded-lg bg-bg-2 px-3 py-2 text-[13px] text-fg-1">docker compose logs nido</pre>
      <Input
        label={t('code.field')}
        name="setup-code"
        value={code}
        onChange={(event) => setCode(event.target.value)}
        autoComplete="off"
        spellCheck={false}
        maxLength={20}
        autoFocus
      />
      {error && <Alert variant="error" className="mt-4">{t(error)}</Alert>}
      <Button type="submit" isLoading={busy} className={`mt-6 ${PRIMARY_CLASS}`} style={CTA_BUTTON_STYLE}>
        {t('action.next')}
      </Button>
    </form>
  )
}
