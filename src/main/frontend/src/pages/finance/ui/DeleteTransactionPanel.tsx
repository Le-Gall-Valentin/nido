import { useTranslation } from 'react-i18next'
import { ConfirmDeleteModal } from '@/shared/ui'
import { useDeleteTransaction, type Transaction } from '@/entities/finance'

interface DeleteTransactionPanelProps {
  spaceId: string
  transaction: Transaction
  onClose: () => void
}

/** The confirmation and the call it makes. Unmounting on close is what resets it. */
export function DeleteTransactionPanel({ spaceId, transaction, onClose }: DeleteTransactionPanelProps) {
  const { t } = useTranslation('finance')
  const deleteTransaction = useDeleteTransaction(spaceId)

  return (
    <ConfirmDeleteModal
      title={t('delete_confirm.title', { label: transaction.label })}
      message={t('delete_confirm.message')}
      confirmLabel={t('delete_confirm.confirm')}
      cancelLabel={t('delete_confirm.cancel')}
      isPending={deleteTransaction.isPending}
      error={deleteTransaction.isError ? t('delete_confirm.error') : null}
      onCancel={onClose}
      onConfirm={() => deleteTransaction.mutate(transaction.id, { onSuccess: onClose })}
    />
  )
}
