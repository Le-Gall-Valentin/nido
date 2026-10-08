import React, { useEffect, useId, useRef, useState } from 'react'
import { AlertTriangle, CheckCircle2, ChevronLeft, Info, Lock, Mail } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { TwoFactorMethod, User } from '@/entities/user'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { CodeInput, type CodeInputHandle } from './CodeInput'
import { ResendCode } from './ResendCode'
import { useResend } from '../model/useResend'
import { commonErrorMessage, minutesOf, type Message } from '../model/messages'
import type { ITwoFactorChallengeApi } from '../model/ITwoFactorChallengeApi'
import {
  ChallengeExpiredError, CodeError, MaxAttemptsError, MethodNotEnabledError, MethodUnavailableError,
} from '../model/errors'

interface CodeStepProps {
  username: string
  method: TwoFactorMethod
  /** MAIL: where the code went, masked. */
  maskedEmail?: string | null
  /** MAIL: seconds before another code can be asked for. */
  resendAfterSeconds?: number
  /** MAIL: the login could not send the code — the account's limit — and when one can leave. */
  mailLimitSeconds?: number | null
  api: Pick<ITwoFactorChallengeApi, 'verify' | 'sendMailCode'>
  onVerified: (user: User) => void
  /** Back to the identifiers; also where a lockout or a method gone leads after a moment. */
  onBack: () => void
  /** Back to the choice, under the same challenge — given only when both methods are on. */
  onChooseAnother?: () => void
}

type Notice = Message & { tone: 'error' | 'ok' }

/** These leave nothing to try on this screen: the sign-in starts again after a moment. */
const RESTARTING = new Set(['verify.error.max_attempts', 'verify.error.method_not_enabled', 'verify.error.method_unavailable'])

function messageOf(error: unknown): Message {
  if (error instanceof CodeError) return { key: 'verify.error.invalid_code' }
  if (error instanceof MaxAttemptsError) return { key: 'verify.error.max_attempts' }
  if (error instanceof ChallengeExpiredError) return { key: 'verify.error.challenge_expired' }
  if (error instanceof MethodNotEnabledError) return { key: 'verify.error.method_not_enabled' }
  if (error instanceof MethodUnavailableError) return { key: 'verify.error.method_unavailable' }
  return commonErrorMessage(error)
}

export function CodeStep({
  username, method, maskedEmail = null, resendAfterSeconds = 0, mailLimitSeconds = null,
  api, onVerified, onBack, onChooseAnother,
}: CodeStepProps) {
  const { t } = useTranslation('twoFactor')
  const headingId = useId()
  const mail = method === 'MAIL'
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [notice, setNotice] = useState<Notice | null>(() =>
    mailLimitSeconds !== null
      ? { tone: 'error', key: 'twoFactor:error.send_limit', values: { minutes: minutesOf(mailLimitSeconds) } }
      : null)
  const [autoBack, setAutoBack] = useState(false)
  // Held back by the account's limit at sign-in, the code has not left until a resend sends it.
  const [codeLeft, setCodeLeft] = useState(mailLimitSeconds === null)
  const { seconds, sending, resend } = useResend(mailLimitSeconds ?? resendAfterSeconds,
    async () => (await api.sendMailCode()).resendAfterSeconds)
  const isSubmittingRef = useRef(false)
  const inputRef = useRef<CodeInputHandle>(null)

  useEffect(() => {
    if (!autoBack) return
    const timer = setTimeout(() => onBack(), 2000)
    return () => clearTimeout(timer)
  }, [autoBack, onBack])

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    if (code.length < 6) { setNotice({ tone: 'error', key: 'verify.error.incomplete' }); return }
    if (isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setNotice(null)
    try {
      onVerified(await api.verify(method, code))
    } catch (error) {
      showError(error)
      if (error instanceof CodeError) {
        setCode('')
        inputRef.current?.focus()
      }
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  function showError(error: unknown) {
    const message = messageOf(error)
    setNotice({ tone: 'error', ...message })
    if (RESTARTING.has(message.key)) setAutoBack(true)
  }

  async function handleResend(): Promise<void> {
    setNotice(null)
    const outcome = await resend()
    if (outcome?.kind === 'sent') {
      setCode('')
      setCodeLeft(true)
      setNotice({ tone: 'ok', key: 'mail.resent' })
    } else if (outcome?.kind === 'recent') {
      // Another tab of this browser signed in moments ago: its code is the live one, already in the mailbox.
      setCodeLeft(true)
      setNotice({ tone: 'ok', key: 'mail.recent' })
    } else if (outcome?.kind === 'failed') {
      showError(outcome.error)
    }
  }

  return (
    <div>
      <button
        type="button"
        onClick={onChooseAnother ?? onBack}
        disabled={isLoading}
        className="mb-5 flex items-center gap-1 bg-transparent border-0 p-0 text-[13.5px] text-fg-2 cursor-pointer hover:text-fg-0"
      >
        <ChevronLeft className="size-[15px]" />
        {t(onChooseAnother ? 'verify.back_choose' : 'verify.back')}
      </button>

      <div className="mb-[18px] grid size-[46px] place-items-center rounded-[13px] bg-accent-dim text-accent">
        {mail ? <Mail className="size-6" /> : <Lock className="size-6" />}
      </div>

      <div className="mb-6">
        <h2 id={headingId} className="mb-1.5 text-[24px] lg:text-[28px] font-semibold tracking-tight text-fg-0">
          {t(mail ? 'mail.title' : 'verify.title')}
        </h2>
        <p className="text-[15px] text-fg-2">
          {mail
            // A code held back has not left: say so rather than send them to their mailbox.
            ? t(codeLeft ? 'mail.subtitle' : 'mail.subtitle_not_sent', { username, address: maskedEmail })
            : t('verify.subtitle', { username })}
        </p>
      </div>

      <form onSubmit={(e) => void handleSubmit(e)} aria-labelledby={headingId}>
        <CodeInput
          ref={inputRef}
          value={code}
          onChange={setCode}
          disabled={isLoading}
          autoFocus
          label={t(mail ? 'mail.code_label' : 'verify.code_label')}
        />

        {notice && (
          <div
            role={notice.tone === 'ok' ? 'status' : 'alert'}
            className={`mb-3 flex items-center gap-2 rounded-[10px] px-3.5 py-[11px] text-[13.5px] ${
              notice.tone === 'ok' ? 'bg-status-green-dim text-status-green' : 'bg-status-red-dim text-status-red'
            }`}
          >
            {notice.tone === 'ok' ? <CheckCircle2 className="size-3.5 shrink-0" /> : <AlertTriangle className="size-3.5 shrink-0" />}
            {t(notice.key, notice.values)}
          </div>
        )}

        <Button
          type="submit"
          isLoading={isLoading}
          className="mt-2 w-full rounded-[11px] border-transparent py-3.5 text-[15px] font-semibold active:translate-y-px disabled:cursor-wait"
          style={CTA_BUTTON_STYLE}
        >
          {t('verify.submit')}
        </Button>
      </form>

      {mail && <ResendCode seconds={seconds} onResend={() => void handleResend()} disabled={isLoading || sending} />}

      <div className="mt-7 flex gap-2 items-start text-[13px] text-fg-2">
        <Info className="size-3.5 shrink-0 mt-0.5" />
        <span>{t(mail ? 'mail.help' : 'verify.help')}</span>
      </div>
    </div>
  )
}
