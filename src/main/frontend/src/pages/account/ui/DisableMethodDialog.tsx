import React, { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import { Dialog, Button } from '@/shared/ui'
import type { TwoFactorMethod } from '@/entities/user'
import {
  CodeError, CodeExpiredError, CodeInput, CodeSpentError, MaxAttemptsError, MethodNotEnabledError, ResendCode,
  commonErrorMessage, useResend, type CodeInputHandle, type ITwoFactorMethodsApi, type Message,
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
  /** Opened for a paused method, but mail came back meanwhile: the method now wants its code, which this dialog has no field for. */
  onStale: () => void
}

function messageOf(error: unknown, method: TwoFactorMethod): Message {
  if (error instanceof CodeError) return { key: 'disable.error.invalid_code' }
  if (error instanceof CodeExpiredError) return { key: 'disable.error.code_expired' }
  // Spent: the mail's code is gone, so a new one is asked for; the app's codes are refused for a quarter of an hour.
  if (error instanceof CodeSpentError) return { key: method === 'APP' ? 'disable.error.code_spent_app' : 'disable.error.code_spent' }
  if (error instanceof MaxAttemptsError) return { key: 'disable.error.max_attempts' }
  return commonErrorMessage(error)
}

export function DisableMethodDialog({ method, paused, address, resendAfterSeconds, api, onClose, onSuccess, onStale }: DisableMethodDialogProps) {
  const { t } = useTranslation('twoFactor')
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Message | null>(null)
  const { seconds, sending, resend } = useResend(resendAfterSeconds, async () => (await api.sendDisableCode()).resendAfterSeconds)
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
      if (e instanceof CodeError && paused) { onStale(); return }
      setCode('')
      setError(messageOf(e, method))
      if (e instanceof CodeError) inputRef.current?.focus()
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  async function handleResend() {
    setError(null)
    const outcome = await resend()
    if (outcome?.kind === 'failed') setError(messageOf(outcome.error, method))
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
          <ResendCode seconds={seconds} onResend={() => void handleResend()} disabled={isLoading || sending} />
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
