import React, { useEffect, useId, useRef, useState } from 'react'
import { AlertTriangle, Check, Eye, EyeOff } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { QRCodeSVG } from 'qrcode.react'
import { Button, Spinner, CTA_BUTTON_STYLE } from '@/shared/ui'
import { TotpDigitInput } from './TotpDigitInput'
import type { TotpDigitInputHandle } from './TotpDigitInput'
import { TotpCodeError, TotpConfirmMaxAttemptsError } from '../model/errors'
import { RateLimitError, NetworkError, ServerError } from '@/shared/lib'
import type { ITotpEnrollApi } from '../model/ITotpEnrollApi'
import type { TotpSetupData } from '../model/types'

interface TotpSetupFlowProps {
  api: ITotpEnrollApi
  onSuccess: () => void
  onDismiss?: () => void
  dismissLabel?: string
}

export function TotpSetupFlow({ api, onSuccess, onDismiss, dismissLabel }: TotpSetupFlowProps) {
  const { t } = useTranslation('totp')
  const headingId = useId()
  const [setupData, setSetupData] = useState<TotpSetupData | null>(null)
  const [setupError, setSetupError] = useState(false)
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const [isSecretVisible, setIsSecretVisible] = useState(false)
  const [setupRestartKey, setSetupRestartKey] = useState(0)
  const isSubmittingRef = useRef(false)
  const digitInputRef = useRef<TotpDigitInputHandle>(null)
  const restartTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const apiRef = useRef(api)

  useEffect(() => { apiRef.current = api }, [api])

  useEffect(() => {
    return () => {
      if (restartTimerRef.current !== null) clearTimeout(restartTimerRef.current)
    }
  }, [])

  useEffect(() => {
    if (!isSecretVisible) return
    const timer = setTimeout(() => setIsSecretVisible(false), 5 * 60 * 1000)
    return () => clearTimeout(timer)
  }, [isSecretVisible])

  useEffect(() => {
    let cancelled = false
    apiRef.current.setup()
      .then(data => {
        if (cancelled) return
        if (!data.secret?.trim()) { setSetupError(true); return }
        setSetupData(data)
      })
      .catch(() => { if (!cancelled) setSetupError(true) })
    return () => { cancelled = true }
  }, [setupRestartKey])

  const groupedSecret = setupData?.secret.match(/.{1,4}/g)?.join(' ') ?? ''

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    if (code.length < 6 || isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await api.confirm(code)
      onSuccess()
    } catch (error) {
      if (error instanceof TotpConfirmMaxAttemptsError) {
        setErrorKey('setup.error.max_attempts')
        setCode('')
        restartTimerRef.current = setTimeout(() => {
          restartTimerRef.current = null
          setSetupData(null)
          setErrorKey(null)
          setSetupRestartKey(k => k + 1)
        }, 2000)
      } else if (error instanceof TotpCodeError) {
        setErrorKey('setup.error.invalid_code')
        setCode('')
        digitInputRef.current?.focusFirst()
      } else if (error instanceof RateLimitError) {
        setErrorKey('setup.error.rate_limit')
      } else if (error instanceof NetworkError) {
        setErrorKey('setup.error.network')
      } else if (error instanceof ServerError) {
        setErrorKey('setup.error.server')
      } else {
        setErrorKey('setup.error.invalid_code')
        setCode('')
        digitInputRef.current?.focusFirst()
      }
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  if (setupError) {
    return (
      <div role="alert" className="text-sm text-status-red text-center py-4">
        {t('setup.error.setup_failed')}
      </div>
    )
  }

  if (!setupData) {
    return <Spinner fullscreen={false} label={t('setup.loading')} />
  }

  return (
    <div>
      <div className="mb-8">
        <h2 id={headingId} className="mb-2 text-[24px] lg:text-[28px] font-semibold tracking-tight text-fg-0">
          {t('setup.title')}
        </h2>
        <p className="text-sm text-fg-2">{t('setup.subtitle')}</p>
      </div>

      <div className="flex gap-4 items-center rounded-lg border border-border bg-bg-2 p-3.5">
        <div className="size-35 shrink-0 bg-white p-2 rounded-md flex items-center justify-center">
          <QRCodeSVG value={setupData.otpauthUri} size={124} level="M" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-[10px] uppercase tracking-[0.08em] text-fg-3 font-semibold mb-1.5">
            {t('setup.manual_label')}
          </div>
          <div className="relative">
            <div
              // select-none prevents accidental clipboard access when hidden.
              // When visible, the secret IS in the DOM as plain text (unavoidable for
              // copy-paste UX). JS/extensions can access it — accepted trade-off.
              className={`text-[13px] text-fg-0 bg-bg-3 border border-border rounded p-2 break-words leading-relaxed font-mono${isSecretVisible ? '' : ' select-none'}`}
              aria-label={t('setup.manual_label')}
            >
              {isSecretVisible ? groupedSecret : '••••••••••••••••'}
            </div>
            <button
              type="button"
              onClick={() => setIsSecretVisible(v => !v)}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-fg-2 hover:text-fg-0"
              aria-label={isSecretVisible ? t('setup.hide_secret') : t('setup.show_secret')}
            >
              {isSecretVisible ? <EyeOff size={14} /> : <Eye size={14} />}
            </button>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit} aria-labelledby={headingId} className="mt-4">
        <TotpDigitInput
          ref={digitInputRef}
          value={code}
          onChange={setCode}
          disabled={isLoading}
          autoFocus
          groupLabel={t('setup.code_group_label')}
          digitLabel={(i) => t('setup.digit_label', { n: i + 1 })}
        />

        {errorKey && (
          <div
            role="alert"
            className="flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red mb-3"
          >
            <AlertTriangle className="size-3.5 shrink-0" />
            {t(errorKey)}
          </div>
        )}

        <Button
          type="submit"
          isLoading={isLoading}
          className="mt-2 w-full border-transparent py-3 font-semibold active:translate-y-px disabled:cursor-wait"
          style={CTA_BUTTON_STYLE}
        >
          <Check className="size-3.5" />
          {t('setup.submit')}
        </Button>

        {onDismiss && (
          <button
            type="button"
            onClick={onDismiss}
            className="w-full mt-2 bg-transparent border-0 text-fg-2 text-xs cursor-pointer py-1.5 hover:text-fg-0 text-center"
          >
            {dismissLabel ?? t('setup.dismiss_login')}
          </button>
        )}
      </form>
    </div>
  )
}