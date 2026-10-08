import React, { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import { Button, CTA_BUTTON_STYLE, Dialog } from '@/shared/ui'
import {
  CodeInput, ResendCode, commonErrorMessage, useResend, type CodeInputHandle, type Message,
} from '@/features/two-factor'
import { EmailCodeExpiredError, EmailCodeInvalidError, EmailCodeSpentError } from '../api/accountApi'

interface EmailCodeDialogProps {
  sentTo: string
  previousAddress: string
  resendAfterSeconds: number
  onConfirm: (code: string) => Promise<void>
  /** @returns the seconds before another code can be asked for */
  onResend: () => Promise<number>
  onCancel: () => void
}

function messageOf(error: unknown): Message {
  if (error instanceof EmailCodeInvalidError) return { key: 'profile.email_code.error.invalid_code' }
  if (error instanceof EmailCodeExpiredError) return { key: 'profile.email_code.error.expired' }
  if (error instanceof EmailCodeSpentError) return { key: 'profile.email_code.error.spent' }
  return commonErrorMessage(error)
}

/** The code sent to the address the account is moving to: nothing is saved before it. */
export function EmailCodeDialog({ sentTo, previousAddress, resendAfterSeconds, onConfirm, onResend, onCancel }: EmailCodeDialogProps) {
  const { t } = useTranslation('account')
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Message | null>(null)
  const { seconds, sending, resend } = useResend(resendAfterSeconds, onResend)
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
      setError(messageOf(e))
      if (e instanceof EmailCodeInvalidError) inputRef.current?.focus()
    } finally {
      setIsLoading(false)
    }
  }

  async function handleResend() {
    setError(null)
    const outcome = await resend()
    if (outcome?.kind === 'sent') setCode('')
    else if (outcome?.kind === 'failed') setError(messageOf(outcome.error))
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
      <ResendCode seconds={seconds} onResend={() => void handleResend()} disabled={isLoading || sending} />
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
