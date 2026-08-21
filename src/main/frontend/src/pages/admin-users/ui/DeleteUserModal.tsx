import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, Trash2 } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
import type { AdminUser } from '@/entities/user'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'

interface DeleteUserModalProps {
  user: AdminUser
  onClose: () => void
  onDelete: (id: string) => Promise<void>
  onSuccess: () => void
}

export function DeleteUserModal({ user, onClose, onDelete, onSuccess }: DeleteUserModalProps) {
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
      await onDelete(user.id)
      onSuccess()
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'delete'))
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
    <Dialog open onClose={handleClose} title={t('delete.title', { username: user.username })}>
      <div className="mb-[15px] grid size-[46px] place-items-center rounded-[13px] bg-status-red-dim text-status-red">
        <AlertTriangle className="size-6" />
      </div>
      <div className="mb-5">
        <h3 className="text-[19px] font-semibold text-fg-0 mb-2">
          {t('delete.title', { username: user.username })}
        </h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('delete.body')}</p>
      </div>

      <div className="mb-5 rounded-[10px] bg-status-red-dim px-3.5 py-2.5 text-sm text-status-red">
        <span className="font-medium">{user.username}</span>
        {' '}·{' '}
        <span className="font-mono text-xs">{user.email}</span>
        {' '}·{' '}
        <span className="font-semibold">{t(`user.role.${user.role}`, { ns: 'shell' })}</span>
      </div>

      {errorKey && (
        <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={handleClose} disabled={isLoading}>
          {t('delete.cancel')}
        </Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-transparent font-semibold text-bg-0"
          style={{ background: 'var(--color-status-red)' }}
        >
          <Trash2 className="size-4" />
          {t('delete.submit')}
        </Button>
      </div>
    </Dialog>
  )
}
