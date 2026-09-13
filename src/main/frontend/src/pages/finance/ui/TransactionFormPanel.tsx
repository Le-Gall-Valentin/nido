import { useTranslation } from 'react-i18next'
import type { SpaceMember } from '@/entities/space'
import {
  useCreateTransaction, useCreateRecurringSeries, useUpdateTransaction,
  type Category, type Transaction,
} from '@/entities/finance'
import { TransactionFormModal, type TransactionFormInput } from './TransactionFormModal'

interface TransactionFormPanelProps {
  spaceId: string
  spaceTimezone: string | undefined
  /** The transaction being edited, or null when creating one. */
  transaction: Transaction | null
  categories: Category[]
  members: SpaceMember[]
  canPickContributors: boolean
  currentUserId: string | null
  onClose: () => void
}

/**
 * Writing an operation: which of the three calls a submission turns into, and what to say when one
 * fails.
 *
 * <p>A submitted form is an edit, a one-off operation, or a recurring series, and only its own shape
 * says which — the header and the operations list open this without knowing. The three mutations, the
 * reset on close and the error line belong with that decision rather than with the page.
 */
export function TransactionFormPanel({
  spaceId, spaceTimezone, transaction, categories, members, canPickContributors, currentUserId, onClose,
}: TransactionFormPanelProps) {
  const { t } = useTranslation('finance')
  const createTransaction = useCreateTransaction(spaceId)
  const createRecurringSeries = useCreateRecurringSeries(spaceId)
  const updateTransaction = useUpdateTransaction(spaceId)

  function handleSubmit(input: TransactionFormInput) {
    if (transaction) {
      updateTransaction.mutate(
        { transactionId: transaction.id, label: input.label, amount: input.amount, type: input.type,
          categoryId: input.categoryId, date: input.date, payerId: input.payerId, contributors: input.contributors },
        { onSuccess: onClose }
      )
      return
    }
    if (input.recurrence) {
      createRecurringSeries.mutate(
        { label: input.label, amount: input.amount, type: input.type, categoryId: input.categoryId, payerId: input.payerId,
          contributors: input.contributors, recurrence: input.recurrence },
        { onSuccess: onClose }
      )
      return
    }
    createTransaction.mutate(
      { label: input.label, amount: input.amount, type: input.type, categoryId: input.categoryId, date: input.date,
        payerId: input.payerId, contributors: input.contributors },
      { onSuccess: onClose }
    )
  }

  const failed = createTransaction.isError || updateTransaction.isError || createRecurringSeries.isError

  return (
    <TransactionFormModal
      spaceTimezone={spaceTimezone}
      mode={transaction ? 'edit' : 'create'}
      transaction={transaction ?? undefined}
      categories={categories}
      members={members}
      canPickContributors={canPickContributors}
      currentUserId={currentUserId}
      onSubmit={handleSubmit}
      onCancel={onClose}
      submitError={failed ? t('form.submit_error') : null}
    />
  )
}
