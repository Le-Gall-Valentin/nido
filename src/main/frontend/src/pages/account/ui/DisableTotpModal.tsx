import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import { Dialog, Button } from '@/shared/ui'
import { TotpDigitInput, TotpCodeError, TotpDisableMaxAttemptsError } from '@/features/totp'
import type { TotpDigitInputHandle } from '@/features/totp'
import { RateLimitError, NetworkError, ServerError } from '@/shared/lib'

interface DisableTotpModalProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  onDisable: (code: string) => Promise<void>
}

export function DisableTotpModal({ open, onClose, onSuccess, onDisable }: DisableTotpModalProps) {
  const { t } = useTranslation('totp')
  const [code, setCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const digitInputRef = useRef<TotpDigitInputHandle>(null)
  const isSubmittingRef = useRef(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (code.length < 6 || isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onDisable(code)
      setCode('')
      onSuccess()
    } catch (error) {
      if (error instanceof TotpCodeError) {
        setErrorKey('disable.error.invalid_code')
        setCode('')
        digitInputRef.current?.focusFirst()
      } else if (error instanceof TotpDisableMaxAttemptsError) {
        setErrorKey('disable.error.max_attempts')
        setCode('')
      } else if (error instanceof RateLimitError) {
        setErrorKey('disable.error.rate_limit')
      } else if (error instanceof NetworkError) {
        setErrorKey('disable.error.network')
      } else if (error instanceof ServerError) {
        setErrorKey('disable.error.server')
      } else {
        setErrorKey('disable.error.invalid_code')
        setCode('')
        digitInputRef.current?.focusFirst()
      }
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    setCode('')
    setErrorKey(null)
    onClose()
  }

  return (
    <Dialog open={open} onClose={handleClose} title={t('disable.title')} maxWidth="max-w-sm">
      <div className="mb-6">
        <h3 className="text-base font-semibold text-fg-0 mb-1">{t('disable.title')}</h3>
        <p className="text-sm text-fg-2">{t('disable.subtitle')}</p>
      </div>
      <form onSubmit={handleSubmit}>
        <TotpDigitInput
          ref={digitInputRef}
          value={code}
          onChange={setCode}
          disabled={isLoading}
          autoFocus
          groupLabel={t('disable.code_group_label')}
          digitLabel={(i) => t('disable.digit_label', { n: i + 1 })}
        />
        {errorKey && (
          <div role="alert" className="flex items-center gap-2 rounded-lg border border-status-red/25 bg-status-red-dim px-3 py-2.5 text-sm text-status-red mt-3">
            <AlertTriangle className="size-3.5 shrink-0" />
            {t(errorKey)}
          </div>
        )}
        <div className="flex gap-2 justify-end mt-4">
          <Button type="button" onClick={handleClose} disabled={isLoading}>
            {t('setup.dismiss_profile')}
          </Button>
          <Button
            type="submit"
            disabled={code.length < 6}
            isLoading={isLoading}
            className="border-status-red/30 bg-status-red-dim text-status-red hover:bg-status-red/20"
          >
            {t('disable.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}