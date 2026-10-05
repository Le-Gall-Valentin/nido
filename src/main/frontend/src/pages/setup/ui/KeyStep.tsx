import { useEffect, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { copyText } from '@/shared/lib'
import { Alert, Button, CTA_BUTTON_STYLE, Spinner } from '@/shared/ui'
import type { ISetupApi } from '../model/ISetupApi'
import type { SetupKey } from '../model/types'
import { SetupCodeInvalidError } from '../model/errors'
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
  /** The code was refused: Nido restarted and has another one. */
  onCodeExpired: () => void
}

export function KeyStep({ api, code, publicUrl, finishing, finishError, onFinish, onBack, onCodeExpired }: Props) {
  const { t } = useTranslation('setup')
  const checkboxId = useId()
  const [key, setKey] = useState<SetupKey | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [copied, setCopied] = useState<boolean | null>(null)

  useEffect(() => {
    let current = true
    api.encryptionKey(code)
      .then((answer) => { if (current) setKey(answer) })
      .catch((error: unknown) => {
        if (!current) return
        if (error instanceof SetupCodeInvalidError) onCodeExpired()
        else setLoadError(describeSetupError(error))
      })
    return () => { current = false }
  }, [api, code, onCodeExpired])

  async function copy(value: string) {
    setCopied(await copyText(value))
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
          {copied === false && <p role="status" className="mt-2 text-[12.5px] text-fg-2">{t('key.copy_failed')}</p>}
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
