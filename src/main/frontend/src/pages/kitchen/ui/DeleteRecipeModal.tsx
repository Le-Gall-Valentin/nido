import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'

interface DeleteRecipeModalProps {
  recipeName: string
  onClose: () => void
  onDelete: () => Promise<void>
}

export function DeleteRecipeModal({ recipeName, onClose, onDelete }: DeleteRecipeModalProps) {
  const { t } = useTranslation('kitchen')
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
      title={t('delete_confirm.title', { name: recipeName })}
      message={t('delete_confirm.body')}
      confirmLabel={t('delete_confirm.submit')}
      cancelLabel={t('delete_confirm.cancel')}
      isPending={isLoading}
      error={hasError ? t('delete_confirm.error') : null}
      onCancel={onClose}
      onConfirm={() => { void handleSubmit() }}
    />
  )
}
