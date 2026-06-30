import React, { useEffect, useId, useRef, useState } from 'react'
import { AlertTriangle, Check, Info } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { TotpDigitInput } from './TotpDigitInput'
import type { TotpDigitInputHandle } from './TotpDigitInput'
import { TotpChallengeExpiredError, TotpMaxAttemptsError } from '../model/errors'
import { RateLimitError, NetworkError, ServerError } from '@/shared/lib'
import type { ITotpVerifyApi } from '../model/ITotpVerifyApi'
import type { User } from '@/entities/user'

interface TotpVerifyStepProps {
  username: string
  api: ITotpVerifyApi
  onVerified: (user: User) => void
  onBack: () => void
}

export function TotpVerifyStep({ username, api, onVerified, onBack }: TotpVerifyStepProps) {
  const { t } = useTranslation('totp')
  const headingId = useId()
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const [rateLimitSeconds, setRateLimitSeconds] = useState<number | null>(null)
  const [autoBack, setAutoBack] = useState(false)
  const isSubmittingRef = useRef(false)
  const digitInputRef = useRef<TotpDigitInputHandle>(null)

  // After max attempts lockout, redirect automatically after 2 seconds.
  useEffect(() => {
    if (!autoBack) return
    const timer = setTimeout(() => onBack(), 2000)
    return () => clearTimeout(timer)
  }, [autoBack, onBack])

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    if (code.length < 6) { setErrorKey('verify.error.incomplete'); return }
    if (isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      const user = await api.verify(code)
      onVerified(user)
    } catch (error) {
      if (error instanceof TotpMaxAttemptsError) {
        setErrorKey('verify.error.max_attempts')
        setAutoBack(true)
      } else if (error instanceof TotpChallengeExpiredError) {
        setErrorKey('verify.error.challenge_expired')
      } else if (error instanceof RateLimitError) {
        if (error.retryAfterSeconds !== null) {
          setErrorKey('verify.error.rate_limit_timed')
          setRateLimitSeconds(error.retryAfterSeconds)
        } else {
          setErrorKey('verify.error.rate_limit')
        }
      } else if (error instanceof NetworkError) {
        setErrorKey('verify.error.network')
      } else if (error instanceof ServerError) {
        setErrorKey('verify.error.server')
      } else {
        setErrorKey('verify.error.invalid_code')
        setCode('')
        digitInputRef.current?.focusFirst()
      }
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  return (
    <div>
      <div className="mb-8">
        <h2 id={headingId} className="mb-2 text-[24px] lg:text-[28px] font-semibold tracking-tight text-fg-0">
          {t('verify.title')}
        </h2>
        <p className="text-sm text-fg-2">
          {t('verify.subtitle', { username })}
        </p>
      </div>

      <form onSubmit={handleSubmit} aria-labelledby={headingId}>
        <TotpDigitInput
          ref={digitInputRef}
          value={code}
          onChange={setCode}
          disabled={isLoading}
          autoFocus
          groupLabel={t('verify.code_group_label')}
          digitLabel={(i) => t('verify.digit_label', { n: i + 1 })}
        />

        {errorKey && (
          <div
            role="alert"
            className="flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red mb-3"
          >
            <AlertTriangle className="size-3.5 shrink-0" />
            {t(errorKey, rateLimitSeconds !== null ? { seconds: rateLimitSeconds } : undefined)}
          </div>
        )}

        <Button
          type="submit"
          isLoading={isLoading}
          className="mt-2 w-full border-transparent py-3 font-semibold active:translate-y-px disabled:cursor-wait"
          style={CTA_BUTTON_STYLE}
        >
          <Check className="size-3.5" />
          {t('verify.submit')}
        </Button>

        <button
          type="button"
          onClick={onBack}
          disabled={isLoading}
          className="w-full mt-2 bg-transparent border-0 text-fg-2 text-xs cursor-pointer py-1.5 hover:text-fg-0 text-center"
        >
          {t('verify.back')}
        </button>
      </form>

      <div className="mt-7 flex gap-2 items-start text-[13px] text-fg-2">
        <Info className="size-3.5 shrink-0 mt-0.5" />
        <span>{t('verify.help')}</span>
      </div>
    </div>
  )
}