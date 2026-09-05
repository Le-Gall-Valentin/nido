import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'

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
      <p className="text-sm text-fg-2">{t('delete_confirm.message')}</p>
      {error && <p className="mt-2 text-sm text-red-600">{t('delete_confirm.error')}</p>}
      <div className="mt-4 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-md px-4 py-2 text-sm">{t('delete_confirm.cancel')}</button>
        <button type="button" onClick={onConfirm} disabled={isPending} className="rounded-md bg-red-600 px-4 py-2 text-sm text-white disabled:opacity-50">
          {t('delete_confirm.confirm')}
        </button>
      </div>
    </Dialog>
  )
}
