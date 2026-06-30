import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Key } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
import type { AdminUser } from '@/entities/user'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'

interface ResetTotpModalProps {
  user: AdminUser
  onClose: () => void
  onReset: (id: string) => Promise<void>
  onSuccess: () => void
}

export function ResetTotpModal({ user, onClose, onReset, onSuccess }: ResetTotpModalProps) {
  const { t } = useTranslation('adminUsers')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  async function handleSubmit() {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onReset(user.id)
      onSuccess()
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'reset_totp'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    setErrorKey(null)
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('reset_totp.title', { username: user.username })} maxWidth="max-w-[460px]">
      <div className="mb-4">
        <h3 className="text-base font-semibold text-fg-0 mb-1">
          {t('reset_totp.title', { username: user.username })}
        </h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('reset_totp.body')}</p>
      </div>

      <Alert variant="warning" className="mb-5">{t('reset_totp.warning')}</Alert>

      {errorKey && (
        <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={handleClose} disabled={isLoading}>
          {t('reset_totp.cancel')}
        </Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-status-orange/30 bg-status-orange-dim text-status-orange hover:bg-status-orange/20"
        >
          <Key className="size-4" />
          {t('reset_totp.submit')}
        </Button>
      </div>
    </Dialog>
  )
}
