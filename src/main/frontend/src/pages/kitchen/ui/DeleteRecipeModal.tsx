import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, Trash2 } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'

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
    <Dialog open onClose={onClose} title={t('delete_confirm.title', { name: recipeName })}>
      <div className="mb-[15px] grid size-[46px] place-items-center rounded-[13px] bg-status-red-dim text-status-red">
        <AlertTriangle className="size-6" />
      </div>
      <div className="mb-5">
        <h3 className="text-[19px] font-semibold text-fg-0 mb-2">{t('delete_confirm.title', { name: recipeName })}</h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('delete_confirm.body')}</p>
      </div>

      {hasError && <Alert variant="error" className="mb-4">{t('delete_confirm.error')}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onClose} disabled={isLoading}>
          {t('delete_confirm.cancel')}
        </Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-transparent font-semibold !text-bg-0 transition hover:brightness-90"
          style={{ background: 'var(--color-status-red)' }}
        >
          <Trash2 className="size-4" />
          {t('delete_confirm.submit')}
        </Button>
      </div>
    </Dialog>
  )
}
