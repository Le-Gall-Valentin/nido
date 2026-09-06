import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'

interface DeleteSpaceModalProps {
  spaceName: string
  onClose: () => void
  onDelete: () => Promise<void>
  onSuccess: () => void
}

export function DeleteSpaceModal({ spaceName, onClose, onDelete, onSuccess }: DeleteSpaceModalProps) {
  const { t } = useTranslation('spaces')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string[] | null>(null)
  const pendingRef = useRef(false)

  async function handleSubmit() {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onDelete()
      onSuccess()
    } catch (error) {
      setErrorKey(mapSpaceErrorToKey(error, 'delete'))
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
      title={t('delete.title', { name: spaceName })}
      message={t('delete.body')}
      confirmLabel={t('delete.submit')}
      cancelLabel={t('delete.cancel')}
      isPending={isLoading}
      error={errorKey ? t(errorKey) : null}
      onCancel={handleClose}
      onConfirm={() => { void handleSubmit() }}
    />
  )
}
