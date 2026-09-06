import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
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
    <ConfirmDeleteModal
      title={t('delete.title', { username: user.username })}
      message={t('delete.body')}
      confirmLabel={t('delete.submit')}
      cancelLabel={t('delete.cancel')}
      isPending={isLoading}
      error={errorKey ? t(errorKey) : null}
      onCancel={handleClose}
      onConfirm={() => { void handleSubmit() }}
    >
      <div className="mb-5 rounded-[10px] bg-status-red-dim px-3.5 py-2.5 text-sm text-status-red">
        <span className="font-medium">{user.username}</span>
        {' '}·{' '}
        <span className="font-mono text-xs">{user.email}</span>
        {' '}·{' '}
        <span className="font-semibold">{t(`user.role.${user.role}`, { ns: 'shell' })}</span>
      </div>
    </ConfirmDeleteModal>
  )
}
