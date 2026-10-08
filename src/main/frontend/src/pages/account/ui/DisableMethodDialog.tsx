import React, { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import { Dialog, Button } from '@/shared/ui'
import { NetworkError, RateLimitError } from '@/shared/lib'
import type { TwoFactorMethod } from '@/entities/user'
import {
  CodeError, CodeInput, MaxAttemptsError, MethodNotEnabledError, ResendCode, ResendTooSoonError, SendLimitError,
  useResendCountdown, type CodeInputHandle, type ITwoFactorMethodsApi,
} from '@/features/two-factor'

interface DisableMethodDialogProps {
  method: TwoFactorMethod
  /** The mail while mail is off: it protects nothing now and has no code to give — it goes without one. */
  paused: boolean
  address: string
  /** MAIL: the code was sent when the row's button was pressed; seconds before another. */
  resendAfterSeconds: number
  api: Pick<ITwoFactorMethodsApi, 'disable' | 'sendDisableCode'>
  onClose: () => void
  onSuccess: () => void
}

export function DisableMethodDialog({ method, paused, address, resendAfterSeconds, api, onClose, onSuccess }: DisableMethodDialogProps) {
  const { t } = useTranslation('twoFactor')
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<{ key: string; values?: Record<string, unknown> } | null>(null)
  const countdown = useResendCountdown(resendAfterSeconds)
  const inputRef = useRef<CodeInputHandle>(null)
  const isSubmittingRef = useRef(false)
  const needsCode = !paused
  const title = t(method === 'APP' ? 'disable.title_app' : 'disable.title_mail')
  const subtitle = paused ? t('disable.paused') : method === 'APP' ? t('disable.subtitle') : t('disable.subtitle_mail', { address })

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if ((needsCode && code.length < 6) || isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setError(null)
    try {
      await api.disable(method, needsCode ? code : undefined)
      onSuccess()
    } catch (e) {
      // Already off — another tab, or an administrator: what was asked is done.
      if (e instanceof MethodNotEnabledError) { onSuccess(); return }
      setCode('')
      if (e instanceof CodeError) { setError({ key: 'disable.error.invalid_code' }); inputRef.current?.focus() }
      else if (e instanceof MaxAttemptsError) setError({ key: 'disable.error.max_attempts' })
      else if (e instanceof RateLimitError) setError({ key: 'disable.error.rate_limit' })
      else if (e instanceof NetworkError) setError({ key: 'disable.error.network' })
      else setError({ key: 'disable.error.server' })
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  async function handleResend() {
    setError(null)
    try {
      countdown.restart((await api.sendDisableCode()).resendAfterSeconds)
    } catch (e) {
      if (e instanceof ResendTooSoonError) countdown.restart(e.seconds)
      else if (e instanceof SendLimitError) {
        countdown.restart(e.seconds)
        setError({ key: 'disable.error.send_limit', values: { minutes: Math.ceil(e.seconds / 60) } })
      } else if (e instanceof NetworkError) setError({ key: 'disable.error.network' })
      else setError({ key: 'disable.error.server' })
    }
  }

  return (
    <Dialog open onClose={onClose} title={title} maxWidth="max-w-sm">
      <div className="mb-6">
        <h3 className="text-xl font-semibold text-fg-0 mb-1.5">{title}</h3>
        <p className="text-sm text-fg-2">{subtitle}</p>
      </div>
      <form onSubmit={(e) => void handleSubmit(e)}>
        {needsCode && (
          <CodeInput ref={inputRef} value={code} onChange={setCode} disabled={isLoading} autoFocus label={t('disable.code_label')} />
        )}
        {error && (
          <div role="alert" className="flex items-center gap-2 rounded-[10px] bg-status-red-dim px-3.5 py-[11px] text-[13.5px] text-status-red mt-3">
            <AlertTriangle className="size-3.5 shrink-0" />
            {t(error.key, error.values)}
          </div>
        )}
        {needsCode && method === 'MAIL' && (
          <ResendCode seconds={countdown.seconds} onResend={() => void handleResend()} disabled={isLoading} />
        )}
        <div className="flex gap-2 justify-end mt-4">
          <Button type="button" onClick={onClose} disabled={isLoading}>{t('setup.dismiss_profile')}</Button>
          <Button
            type="submit"
            disabled={needsCode && code.length < 6}
            isLoading={isLoading}
            className="border-transparent font-semibold !text-bg-0 transition hover:brightness-90"
            style={{ background: 'var(--color-status-red)' }}
          >
            {t('disable.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
