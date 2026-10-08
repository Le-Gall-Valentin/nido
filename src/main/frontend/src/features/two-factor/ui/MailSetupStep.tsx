import React, { useId, useRef, useState } from 'react'
import { AlertTriangle, ChevronLeft, Mail } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { NetworkError, RateLimitError } from '@/shared/lib'
import { CodeInput, type CodeInputHandle } from './CodeInput'
import { ResendCode } from './ResendCode'
import { useResendCountdown } from '../model/useResendCountdown'
import type { ITwoFactorMethodsApi } from '../model/ITwoFactorMethodsApi'
import {
  CodeError, ConfirmMaxAttemptsError, EnrolmentExpiredError, MethodAlreadyEnabledError, MethodUnavailableError,
  ResendTooSoonError, SendLimitError,
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

/** Proving the address receives mail before the mail becomes a way in. */
export function MailSetupStep({ sentTo, resendAfterSeconds, api, onSuccess, onBack, onDismiss, dismissLabel, variant }: MailSetupStepProps) {
  const { t } = useTranslation('twoFactor')
  const headingId = useId()
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<{ key: string; values?: Record<string, unknown> } | null>(null)
  const countdown = useResendCountdown(resendAfterSeconds)
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
      if (e instanceof CodeError) { setError({ key: 'mailSetup.error.invalid_code' }); inputRef.current?.focus() }
      else if (e instanceof ConfirmMaxAttemptsError) { setError({ key: 'mailSetup.error.max_attempts' }); countdown.restart(0) }
      else if (e instanceof EnrolmentExpiredError) { setError({ key: 'mailSetup.error.expired' }); countdown.restart(0) }
      else if (e instanceof MethodUnavailableError) setError({ key: 'mailSetup.error.mail_unavailable' })
      else if (e instanceof RateLimitError) setError({ key: 'mailSetup.error.rate_limit' })
      else if (e instanceof NetworkError) setError({ key: 'mailSetup.error.network' })
      else setError({ key: 'mailSetup.error.server' })
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  async function handleResend(): Promise<void> {
    setError(null)
    try {
      const { resendAfterSeconds: next } = await api.setupMail()
      countdown.restart(next)
      setCode('')
    } catch (e) {
      if (e instanceof ResendTooSoonError) countdown.restart(e.seconds)
      else if (e instanceof SendLimitError) {
        countdown.restart(e.seconds)
        setError({ key: 'mailSetup.error.send_limit', values: { minutes: Math.ceil(e.seconds / 60) } })
      } else if (e instanceof MethodUnavailableError) setError({ key: 'mailSetup.error.mail_unavailable' })
      else if (e instanceof NetworkError) setError({ key: 'mailSetup.error.network' })
      else setError({ key: 'mailSetup.error.server' })
    }
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

      <ResendCode seconds={countdown.seconds} onResend={() => void handleResend()} disabled={isLoading} />

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
