import React, { useId, useRef, useState } from 'react'
import { AlertTriangle, ChevronLeft, Mail } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { CodeInput, type CodeInputHandle } from './CodeInput'
import { ResendCode } from './ResendCode'
import { useResend } from '../model/useResend'
import { commonErrorMessage, type Message } from '../model/messages'
import type { ITwoFactorMethodsApi } from '../model/ITwoFactorMethodsApi'
import {
  CodeError, ConfirmMaxAttemptsError, EnrolmentExpiredError, MethodAlreadyEnabledError, MethodUnavailableError,
} from '../model/errors'

interface MailSetupStepProps {
  sentTo: string
  resendAfterSeconds: number
  api: Pick<ITwoFactorMethodsApi, 'setupMail' | 'confirm'>
  onSuccess: () => void
  /** Page only: back to the choice of method. */
  onBack?: () => void
  onDismiss?: () => void
  dismissLabel?: string
  /** 'page': the login's column, with its heading; 'dialog': the dialog gives the title. */
  variant: 'page' | 'dialog'
}

function messageOf(error: unknown): Message {
  if (error instanceof CodeError) return { key: 'mailSetup.error.invalid_code' }
  if (error instanceof ConfirmMaxAttemptsError) return { key: 'mailSetup.error.max_attempts' }
  if (error instanceof EnrolmentExpiredError) return { key: 'mailSetup.error.expired' }
  if (error instanceof MethodUnavailableError) return { key: 'mailSetup.error.mail_unavailable' }
  return commonErrorMessage(error)
}

/** Proving the address receives mail before the mail becomes a way in. */
export function MailSetupStep({ sentTo, resendAfterSeconds, api, onSuccess, onBack, onDismiss, dismissLabel, variant }: MailSetupStepProps) {
  const { t } = useTranslation('twoFactor')
  const headingId = useId()
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Message | null>(null)
  const { seconds, sending, resend, restart } = useResend(resendAfterSeconds, async () => (await api.setupMail()).resendAfterSeconds)
  const isSubmittingRef = useRef(false)
  const inputRef = useRef<CodeInputHandle>(null)

  async function handleSubmit(event: React.SyntheticEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    if (code.length < 6) { setError({ key: 'mailSetup.error.incomplete' }); return }
    if (isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setError(null)
    try {
      await api.confirm('MAIL', code)
      onSuccess()
    } catch (e) {
      if (e instanceof MethodAlreadyEnabledError) { onSuccess(); return }
      setCode('')
      setError(messageOf(e))
      if (e instanceof CodeError) inputRef.current?.focus()
      // The code is gone, by wrong guesses or by time: a new one can be asked for at once.
      if (e instanceof ConfirmMaxAttemptsError || e instanceof EnrolmentExpiredError) restart(0)
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  async function handleResend(): Promise<void> {
    setError(null)
    const outcome = await resend()
    if (outcome?.kind === 'sent') setCode('')
    else if (outcome?.kind === 'failed') setError(messageOf(outcome.error))
  }

  return (
    <div>
      {variant === 'page' && (
        <>
          {onBack && (
            <button
              type="button"
              onClick={onBack}
              disabled={isLoading}
              className="mb-5 flex items-center gap-1 bg-transparent border-0 p-0 text-[13.5px] text-fg-2 cursor-pointer hover:text-fg-0"
            >
              <ChevronLeft className="size-[15px]" />
              {t('mailSetup.back')}
            </button>
          )}
          <div className="mb-[18px] grid size-[46px] place-items-center rounded-[13px] bg-accent-dim text-accent">
            <Mail className="size-6" />
          </div>
          <h2 id={headingId} className="mb-1.5 text-[24px] lg:text-[27px] font-semibold tracking-tight text-fg-0">
            {t('mailSetup.title')}
          </h2>
        </>
      )}
      <p className="mb-5 text-[14.5px] leading-relaxed text-fg-2">{t('mailSetup.subtitle', { address: sentTo })}</p>

      <form onSubmit={(e) => void handleSubmit(e)} aria-labelledby={variant === 'page' ? headingId : undefined}>
        <CodeInput ref={inputRef} value={code} onChange={setCode} disabled={isLoading} autoFocus label={t('mailSetup.code_label')} />
        {error && (
          <div role="alert" className="mb-3 flex items-center gap-2 rounded-[10px] bg-status-red-dim px-3.5 py-[11px] text-[13.5px] text-status-red">
            <AlertTriangle className="size-3.5 shrink-0" />
            {t(error.key, error.values)}
          </div>
        )}
        <Button
          type="submit"
          isLoading={isLoading}
          className="mt-2 w-full rounded-[11px] border-transparent py-3.5 text-[15px] font-semibold active:translate-y-px disabled:cursor-wait"
          style={CTA_BUTTON_STYLE}
        >
          {t('mailSetup.submit')}
        </Button>
      </form>

      <ResendCode seconds={seconds} onResend={() => void handleResend()} disabled={isLoading || sending} />

      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="w-full mt-2 bg-transparent border-0 text-fg-2 text-xs cursor-pointer py-1.5 hover:text-fg-0 text-center"
        >
          {dismissLabel ?? t('setup.dismiss_login')}
        </button>
      )}
    </div>
  )
}
