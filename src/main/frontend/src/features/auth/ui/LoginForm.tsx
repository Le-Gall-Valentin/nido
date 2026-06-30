import { CredentialsError, RateLimitError, ServerError } from '../model/errors'
import React, { useId, useRef, useState } from 'react'
import { AlertTriangle, Eye, EyeOff, ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../model/authStoreContext'
import { Button, Input, CTA_BUTTON_STYLE, CTA_BUTTON_SHADOW } from '@/shared/ui'
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
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
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
      const outcome = await login({ username, password })
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
    <form onSubmit={handleSubmit} aria-labelledby={labelId} className="flex flex-col gap-4">
      {errorMessage && (
        <div
          id={errorAlertId}
          role="alert"
          className="flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red"
        >
          <AlertTriangle className="size-3.5 shrink-0" />
          {errorMessage}
        </div>
      )}

      <Input
        label={t('field.username')}
        name="username"
        type="text"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder={t('field.username_placeholder')}
        required
        autoComplete="username"
        autoFocus
        aria-invalid={errorKind !== null}
        aria-describedby={errorKind !== null ? errorAlertId : undefined}
      />

      <Input
        label={t('field.password')}
        name="password"
        type={showPassword ? 'text' : 'password'}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder={t('field.password_placeholder')}
        required
        autoComplete="current-password"
        spellCheck={false}
        autoCorrect="off"
        autoCapitalize="off"
        aria-invalid={errorKind !== null}
        aria-describedby={errorKind !== null ? errorAlertId : undefined}
        suffix={
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="rounded-md p-2 text-fg-3 transition-colors hover:bg-bg-2 hover:text-fg-0"
            aria-label={showPassword ? t('field.hide_password') : t('field.show_password')}
          >
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        }
      />

      <Button
        type="submit"
        isLoading={isLoading}
        className="mt-2 w-full border-transparent py-3 font-semibold active:translate-y-px disabled:cursor-wait"
        style={{ ...CTA_BUTTON_STYLE, boxShadow: CTA_BUTTON_SHADOW }}
      >
        {t('action.submit')}
        <ChevronRight className="size-3.5" />
      </Button>
    </form>
  )
}