import { useTranslation } from 'react-i18next'
import { AlertTriangle, Trash2 } from 'lucide-react'
import { Alert, Dialog, Button } from '@/shared/ui'

interface DeleteTransactionModalProps {
  label: string
  onConfirm: () => void
  onCancel: () => void
  isPending: boolean
  error: string | null
}

export function DeleteTransactionModal({ label, onConfirm, onCancel, isPending, error }: DeleteTransactionModalProps) {
  const { t } = useTranslation('finance')
  return (
    <Dialog open onClose={onCancel} title={t('delete_confirm.title', { label })}>
      <div className="mb-[15px] grid size-[46px] place-items-center rounded-[13px] bg-status-red-dim text-status-red">
        <AlertTriangle className="size-6" />
      </div>
      <div className="mb-5">
        <h3 className="mb-2 text-[19px] font-semibold text-fg-0">{t('delete_confirm.title', { label })}</h3>
        <p className="text-sm leading-relaxed text-fg-2">{t('delete_confirm.message')}</p>
      </div>

      {error && <Alert variant="error" className="mb-4">{t('delete_confirm.error')}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onCancel} disabled={isPending}>{t('delete_confirm.cancel')}</Button>
        <Button
          onClick={onConfirm}
          isLoading={isPending}
          className="border-transparent font-semibold !text-bg-0 transition hover:brightness-90"
          style={{ background: 'var(--color-status-red)' }}
        >
          <Trash2 className="size-4" />
          {t('delete_confirm.confirm')}
        </Button>
      </div>
    </Dialog>
  )
}
