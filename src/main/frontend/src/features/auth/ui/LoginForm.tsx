import { CredentialsError, RateLimitError, ServerError } from '../model/errors'
import React, { useId, useRef, useState } from 'react'
import { AlertTriangle, ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../model/authStoreContext'
import { Button, Input, PasswordInput, AUTH_FIELD_CLASS, AUTH_SUBMIT_CLASS, CTA_ELEVATED_STYLE, VERBATIM_INPUT_PROPS } from '@/shared/ui'
import type { LoginOutcome } from '../model/types'

interface LoginFormProps {
  labelId?: string
  onLoginOutcome?: (outcome: Exclude<LoginOutcome, { kind: 'authenticated' }>) => void
}

type ErrorKind = 'credentials' | 'network' | 'rateLimit' | 'server' | null


const ERROR_I18N_KEYS = {
  credentials: 'error.credentials',
  network: 'error.network',
  rateLimit: 'error.rateLimit',
  server: 'error.server',
} as const satisfies Record<NonNullable<ErrorKind>, string>

export function LoginForm({ labelId, onLoginOutcome }: LoginFormProps) {
  const login = useAuth((s) => s.login)
  const { t } = useTranslation('auth')
  const errorAlertId = useId()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [errorKind, setErrorKind] = useState<ErrorKind>(null)
  const [retryAfterSeconds, setRetryAfterSeconds] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const isSubmittingRef = useRef(false)

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    if (isSubmittingRef.current) return
    isSubmittingRef.current = true
    setErrorKind(null)
    setRetryAfterSeconds(null)
    setIsLoading(true)
    try {
      const outcome = await login({ identifier, password })
      if (outcome.kind !== 'authenticated') {
        onLoginOutcome?.(outcome)
      }
    } catch (error) {
      if (error instanceof CredentialsError) {
        setErrorKind('credentials')
        setPassword('')
      } else if (error instanceof RateLimitError) {
        setErrorKind('rateLimit')
        setRetryAfterSeconds(error.retryAfterSeconds)
      } else if (error instanceof ServerError) {
        setErrorKind('server')
      } else {
        setErrorKind('network')
      }
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  const errorMessage = errorKind === null
    ? null
    : errorKind === 'rateLimit' && retryAfterSeconds !== null
      ? t('error.rateLimitWithDelay', { seconds: retryAfterSeconds })
      : t(ERROR_I18N_KEYS[errorKind])

  return (
    <form onSubmit={(e) => void handleSubmit(e)} aria-labelledby={labelId} className="flex flex-col gap-4">
      {errorMessage && (
        <div
          id={errorAlertId}
          role="alert"
          className="flex items-center gap-2 rounded-[10px] bg-status-red-dim px-3.5 py-[11px] text-[13.5px] text-status-red"
        >
          <AlertTriangle className="size-3.5 shrink-0" />
          {errorMessage}
        </div>
      )}

      <Input
        label={t('field.identifier')}
        name="identifier"
        type="text"
        value={identifier}
        onChange={(e) => setIdentifier(e.target.value)}
        placeholder={t('field.identifier_placeholder')}
        required
        maxLength={254}
        // "username" is the token password managers fill — it covers an address typed here too.
        autoComplete="username"
        {...VERBATIM_INPUT_PROPS}
        autoFocus
        className={AUTH_FIELD_CLASS}
        aria-invalid={errorKind !== null}
        aria-describedby={errorKind !== null ? errorAlertId : undefined}
      />

      <PasswordInput
        label={t('field.password')}
        name="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder={t('field.password_placeholder')}
        required
        autoComplete="current-password"
        className={AUTH_FIELD_CLASS}
        aria-invalid={errorKind !== null}
        aria-describedby={errorKind !== null ? errorAlertId : undefined}
      />

      <Button
        type="submit"
        isLoading={isLoading}
        className={`${AUTH_SUBMIT_CLASS} disabled:cursor-wait`}
        style={CTA_ELEVATED_STYLE}
      >
        {t('action.submit')}
        <ChevronRight className="size-3.5" />
      </Button>
    </form>
  )
}