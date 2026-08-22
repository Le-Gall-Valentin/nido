import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { LogOut } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'

interface LeaveSpaceModalProps {
  spaceName: string
  onClose: () => void
  onLeave: () => Promise<void>
  onSuccess: () => void
}

export function LeaveSpaceModal({ spaceName, onClose, onLeave, onSuccess }: LeaveSpaceModalProps) {
  const { t } = useTranslation('spaces')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  async function handleSubmit() {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onLeave()
      onSuccess()
    } catch (error) {
      setErrorKey(mapSpaceErrorToKey(error, 'leave'))
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
    <Dialog open onClose={handleClose} title={t('leave.title', { name: spaceName })} maxWidth="max-w-[460px]">
      <div className="mb-4">
        <h3 className="text-xl font-semibold text-fg-0 mb-1.5">{t('leave.title', { name: spaceName })}</h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('leave.body')}</p>
      </div>

      {errorKey && (
        <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={handleClose} disabled={isLoading}>
          {t('leave.cancel')}
        </Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-transparent font-semibold text-bg-0"
          style={{ background: 'var(--color-status-red)' }}
        >
          <LogOut className="size-4" />
          {t('leave.submit')}
        </Button>
      </div>
    </Dialog>
  )
}
