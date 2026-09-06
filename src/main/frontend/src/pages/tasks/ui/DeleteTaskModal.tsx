import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'

interface DeleteTaskModalProps {
  taskTitle: string
  onClose: () => void
  onDelete: () => Promise<void>
}

export function DeleteTaskModal({ taskTitle, onClose, onDelete }: DeleteTaskModalProps) {
  const { t } = useTranslation('tasks')
  const [isLoading, setIsLoading] = useState(false)
  const [hasError, setHasError] = useState(false)
  const pendingRef = useRef(false)

  async function handleSubmit() {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setHasError(false)
    try {
      await onDelete()
      onClose()
    } catch {
      setHasError(true)
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  return (
    <ConfirmDeleteModal
      title={t('delete_confirm.title', { title: taskTitle })}
      message={t('delete_confirm.message')}
      confirmLabel={t('delete_confirm.confirm')}
      cancelLabel={t('delete_confirm.cancel')}
      isPending={isLoading}
      error={hasError ? t('delete_confirm.error') : null}
      onCancel={onClose}
      onConfirm={() => { void handleSubmit() }}
    />
  )
}
