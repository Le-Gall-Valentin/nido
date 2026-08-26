import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Crown } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'
import type { SpaceMember } from '@/entities/space'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'

interface TransferOwnershipModalProps {
  target: SpaceMember
  onClose: () => void
  onTransfer: (userId: string) => Promise<void>
  onSuccess: () => void
}

export function TransferOwnershipModal({ target, onClose, onTransfer, onSuccess }: TransferOwnershipModalProps) {
  const { t } = useTranslation('spaces')
  const targetName = target.username ?? t('members.deleted_account')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string[] | null>(null)
  const pendingRef = useRef(false)

  async function handleSubmit() {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onTransfer(target.userId)
      onSuccess()
    } catch (error) {
      setErrorKey(mapSpaceErrorToKey(error, 'transfer'))
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
    <Dialog open onClose={handleClose} title={t('transfer.title', { name: targetName })} maxWidth="max-w-[460px]">
      <div className="mb-4">
        <h3 className="text-xl font-semibold text-fg-0 mb-1.5">{t('transfer.title', { name: targetName })}</h3>
        <p className="text-sm text-fg-2 leading-relaxed">{t('transfer.body', { name: targetName })}</p>
      </div>

      <Alert variant="warning" className="mb-5">{t('transfer.warning')}</Alert>

      {errorKey && (
        <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={handleClose} disabled={isLoading}>
          {t('transfer.cancel')}
        </Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-status-orange/30 bg-status-orange-dim text-status-orange hover:bg-status-orange/20"
        >
          <Crown className="size-4" />
          {t('transfer.submit')}
        </Button>
      </div>
    </Dialog>
  )
}
