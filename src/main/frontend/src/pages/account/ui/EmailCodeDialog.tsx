import React, { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import { Button, CTA_BUTTON_STYLE, Dialog } from '@/shared/ui'
import { NetworkError } from '@/shared/lib'
import {
  CodeInput, ResendCode, ResendTooSoonError, SendLimitError, useResendCountdown, type CodeInputHandle,
} from '@/features/two-factor'
import { EmailCodeInvalidError } from '../api/accountApi'

interface EmailCodeDialogProps {
  sentTo: string
  previousAddress: string
  resendAfterSeconds: number
  onConfirm: (code: string) => Promise<void>
  /** @returns the seconds before another code can be asked for */
  onResend: () => Promise<number>
  onCancel: () => void
}

/** The code sent to the address the account is moving to: nothing is saved before it. */
export function EmailCodeDialog({ sentTo, previousAddress, resendAfterSeconds, onConfirm, onResend, onCancel }: EmailCodeDialogProps) {
  const { t } = useTranslation('account')
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<{ key: string; values?: Record<string, unknown> } | null>(null)
  const countdown = useResendCountdown(resendAfterSeconds)
  const inputRef = useRef<CodeInputHandle>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (code.length < 6) { setError({ key: 'profile.email_code.error.incomplete' }); return }
    if (isLoading) return
    setIsLoading(true)
    setError(null)
    try {
      await onConfirm(code)
    } catch (e) {
      setCode('')
      if (e instanceof EmailCodeInvalidError) { setError({ key: 'profile.email_code.error.invalid_code' }); inputRef.current?.focus() }
      else if (e instanceof NetworkError) setError({ key: 'profile.email_code.error.network' })
      else setError({ key: 'profile.email_code.error.server' })
    } finally {
      setIsLoading(false)
    }
  }

  async function handleResend() {
    setError(null)
    try {
      countdown.restart(await onResend())
      setCode('')
    } catch (e) {
      if (e instanceof ResendTooSoonError) countdown.restart(e.seconds)
      else if (e instanceof SendLimitError) {
        countdown.restart(e.seconds)
        setError({ key: 'profile.email_code.error.send_limit', values: { minutes: Math.ceil(e.seconds / 60) } })
      } else if (e instanceof NetworkError) setError({ key: 'profile.email_code.error.network' })
      else setError({ key: 'profile.email_code.error.server' })
    }
  }

  return (
    <Dialog open onClose={onCancel} title={t('profile.email_code.title')} maxWidth="max-w-md">
      <div className="mb-5">
        <h3 className="text-xl font-semibold text-fg-0 mb-1.5">{t('profile.email_code.title')}</h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('profile.email_code.subtitle', { address: sentTo })}</p>
      </div>
      <form onSubmit={(e) => void handleSubmit(e)}>
        <CodeInput ref={inputRef} value={code} onChange={setCode} disabled={isLoading} autoFocus label={t('profile.email_code.code_label')} />
        {error && (
          <div role="alert" className="mb-3 flex items-center gap-2 rounded-[10px] bg-status-red-dim px-3.5 py-[11px] text-[13.5px] text-status-red">
            <AlertTriangle className="size-3.5 shrink-0" />
            {t(error.key, error.values)}
          </div>
        )}
        <Button type="submit" isLoading={isLoading} className="mt-2 w-full border-transparent py-3 font-semibold" style={CTA_BUTTON_STYLE}>
          {t('profile.email_code.submit')}
        </Button>
      </form>
      <ResendCode seconds={countdown.seconds} onResend={() => void handleResend()} disabled={isLoading} />
      <button
        type="button"
        onClick={onCancel}
        className="w-full mt-2 bg-transparent border-0 text-fg-2 text-xs cursor-pointer py-1.5 hover:text-fg-0 text-center"
      >
        {t('profile.email_code.cancel', { address: previousAddress })}
      </button>
    </Dialog>
  )
}
