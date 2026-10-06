import { useTranslation } from 'react-i18next'
import { Key } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
import type { AdminUser } from '@/entities/user'
import type { MailAvailability } from '@/entities/capabilities'
import { useDialogSubmit } from '../lib/useDialogSubmit'
import { MailNotice } from './MailNotice'

interface ResetTotpModalProps {
  user: AdminUser
  mail: MailAvailability
  onClose: () => void
  onReset: (id: string) => Promise<void>
  onSuccess: () => void
}

export function ResetTotpModal({ user, mail, onClose, onReset, onSuccess }: ResetTotpModalProps) {
  const { t } = useTranslation('adminUsers')
  const { submit, isLoading, errorKey, clearError } = useDialogSubmit('reset_totp')

  function handleSubmit() {
    void submit(async () => {
      await onReset(user.id)
      onSuccess()
    })
  }

  function handleClose() {
    clearError()
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('reset_totp.title', { username: user.username })} maxWidth="max-w-[460px]">
      <div className="mb-4">
        <h3 className="text-xl font-semibold text-fg-0 mb-1.5">
          {t('reset_totp.title', { username: user.username })}
        </h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('reset_totp.body')}</p>
      </div>

      <Alert variant="warning" className="mb-5">{t('reset_totp.warning')}</Alert>

      <MailNotice user={user} mail={mail} />

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
