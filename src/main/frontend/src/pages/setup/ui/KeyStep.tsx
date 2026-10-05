import { useEffect, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, CTA_BUTTON_STYLE, Spinner } from '@/shared/ui'
import type { ISetupApi } from '../model/ISetupApi'
import type { SetupKey } from '../model/types'
import { describeSetupError } from '../lib/describeSetupError'
import { downloadKeyFile, keyFileText } from '../lib/keyFile'
import { ACTIONS_CLASS, LEAD_CLASS, TITLE_CLASS } from './styles'

interface Props {
  api: ISetupApi
  code: string
  publicUrl: string
  finishing: boolean
  finishError: string | null
  onFinish: (keySaved: boolean) => void
  onBack: () => void
}

export function KeyStep({ api, code, publicUrl, finishing, finishError, onFinish, onBack }: Props) {
  const { t } = useTranslation('setup')
  const checkboxId = useId()
  const [key, setKey] = useState<SetupKey | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    let current = true
    api.encryptionKey(code)
      .then((answer) => { if (current) setKey(answer) })
      .catch((error: unknown) => { if (current) setLoadError(describeSetupError(error)) })
    return () => { current = false }
  }, [api, code])

  async function copy(value: string) {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  if (loadError) return <Alert variant="error">{t(loadError)}</Alert>
  if (!key) return <Spinner label={t('key.title')} />

  const generated = key.source === 'GENERATED'
  return (
    <div>
      <h1 className={TITLE_CLASS}>{t('key.title')}</h1>
      {generated ? (
        <>
          <p className={LEAD_CLASS}>{t('key.lead')}</p>
          <code className="block break-all rounded-lg bg-bg-2 px-3 py-3 font-mono text-[14px] text-fg-0">{key.key}</code>
          <div className="mt-3 flex gap-2">
            <Button type="button" onClick={() => void copy(key.key)}>{copied ? t('action.copied') : t('action.copy')}</Button>
            <Button type="button" onClick={() => downloadKeyFile(keyFileText(key.key, publicUrl, new Date(), t))}>{t('action.download')}</Button>
          </div>
          <div className="mt-5 flex items-start gap-2.5">
            <input id={checkboxId} type="checkbox" checked={saved} onChange={(event) => setSaved(event.target.checked)} className="mt-1 size-4" />
            <label htmlFor={checkboxId} className="text-sm text-fg-1">{t('key.saved')}</label>
          </div>
        </>
      ) : (
        <p className={LEAD_CLASS}>{t('key.provided')}</p>
      )}
      {finishError && <Alert variant="error" className="mt-4">{t(finishError)}</Alert>}
      <div className={ACTIONS_CLASS}>
        <Button type="button" onClick={onBack}>{t('action.back')}</Button>
        <Button
          type="button"
          className="border-transparent"
          style={CTA_BUTTON_STYLE}
          isLoading={finishing}
          disabled={generated && !saved}
          onClick={() => onFinish(generated && saved)}
        >
          {t('action.finish')}
        </Button>
      </div>
    </div>
  )
}
