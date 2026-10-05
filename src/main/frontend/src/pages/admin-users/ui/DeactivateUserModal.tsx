import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Dialog } from '@/shared/ui'
import type { AdminUser } from '@/entities/user'
import type { MailAvailability } from '@/features/password-reset'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'
import { MailNotice } from './MailNotice'

interface DeactivateUserModalProps {
  user: AdminUser
  mail: MailAvailability
  onClose: () => void
  onDeactivate: (user: AdminUser) => Promise<void>
  onSuccess: () => void
}

/**
 * Deactivating cuts someone off and tells them — and the super-administrators — by mail: confirmed first, so
 * a click that slipped does not send mails for nothing. Reactivating needs no confirmation.
 */
export function DeactivateUserModal({ user, mail, onClose, onDeactivate, onSuccess }: DeactivateUserModalProps) {
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
      await onDeactivate(user)
      onSuccess()
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'deactivate'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  const title = t('deactivate.title', { username: user.username })
  return (
    <Dialog open onClose={onClose} title={title} maxWidth="max-w-[460px]">
      <div className="mb-4">
        <h3 className="mb-1.5 text-xl font-semibold text-fg-0">{title}</h3>
        <p className="text-sm leading-relaxed text-fg-2">{t('deactivate.body', { username: user.username })}</p>
      </div>
      <MailNotice user={user} mail={mail} />
      {errorKey && <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>}
      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onClose} disabled={isLoading}>{t('deactivate.cancel')}</Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-status-orange/30 bg-status-orange-dim text-status-orange hover:bg-status-orange/20"
        >
          {t('deactivate.submit')}
        </Button>
      </div>
    </Dialog>
  )
}
