import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, Trash2 } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
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
    <Dialog open onClose={handleClose} title={t('delete.title', { name: spaceName })}>
      <div className="mb-[15px] grid size-[46px] place-items-center rounded-[13px] bg-status-red-dim text-status-red">
        <AlertTriangle className="size-6" />
      </div>
      <div className="mb-5">
        <h3 className="text-[19px] font-semibold text-fg-0 mb-2">{t('delete.title', { name: spaceName })}</h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('delete.body')}</p>
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
          className="border-transparent font-semibold !text-bg-0 transition hover:brightness-90"
          style={{ background: 'var(--color-status-red)' }}
        >
          <Trash2 className="size-4" />
          {t('delete.submit')}
        </Button>
      </div>
    </Dialog>
  )
}
