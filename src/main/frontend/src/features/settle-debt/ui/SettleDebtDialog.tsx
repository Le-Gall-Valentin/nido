import { useTranslation } from 'react-i18next'
import { useSettleDebt } from '@/entities/finance'
import { SettleDebtModal } from './SettleDebtModal'

/** A debt as the balances show it: who pays whom, and how much is owed. */
export interface DebtToSettle {
  fromMemberId: string
  toMemberId: string
  amount: number
}

interface SettleDebtDialogProps {
  spaceId: string
  /** The household's calendar — the default date has to be its today, not the browser's. */
  spaceTimezone?: string
  debt: DebtToSettle
  memberLabel: (memberId: string) => string
  /** The caller unmounts the dialog: a cancel, and a settlement once it is recorded. */
  onClose: () => void
}

/**
 * Settling a debt, whole: the dialog, the write, what a refusal says, and closing once it is recorded.
 * The dashboard and the finance page open this same flow; neither wires its own.
 */
export function SettleDebtDialog({ spaceId, spaceTimezone, debt, memberLabel, onClose }: SettleDebtDialogProps) {
  const { t } = useTranslation('settleDebt')
  const settleDebt = useSettleDebt(spaceId)
  return (
    <SettleDebtModal
      spaceTimezone={spaceTimezone}
      fromLabel={memberLabel(debt.fromMemberId)}
      toLabel={memberLabel(debt.toMemberId)}
      amount={debt.amount}
      isPending={settleDebt.isPending}
      onCancel={onClose}
      onConfirm={(amount, date) => settleDebt.mutate(
        { fromMemberId: debt.fromMemberId, toMemberId: debt.toMemberId, amount, date },
        { onSuccess: onClose },
      )}
      submitError={settleDebt.isError ? t('submit_error') : null}
    />
  )
}
