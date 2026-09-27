import { useTranslation } from 'react-i18next'
import { useSettleDebt } from '@/entities/finance'
import { useSpaceTimezone } from '@/features/space-switcher'
import { SettleDebtModal } from '@/features/settle-debt'
import { useDashboardActions, type PendingSettlement } from '../model/dashboardActions'

/** Settling what the caller owes, with the finance page's own dialog; the dashboard refreshes after it. */
export function SettleDebtFlow({ debt, onClose }: { debt: PendingSettlement; onClose: () => void }) {
  const { t } = useTranslation('dashboard')
  const { spaceId, currentUserId, memberName } = useDashboardActions()
  const settleDebt = useSettleDebt(spaceId)
  const spaceTimezone = useSpaceTimezone(spaceId)
  if (!currentUserId) return null
  return (
    <SettleDebtModal
      spaceTimezone={spaceTimezone}
      fromLabel={memberName(currentUserId)}
      toLabel={memberName(debt.toMemberId)}
      amount={debt.amount}
      isPending={settleDebt.isPending}
      onCancel={onClose}
      onConfirm={(amount, date) => settleDebt.mutate(
        { fromMemberId: currentUserId, toMemberId: debt.toMemberId, amount, date },
        { onSuccess: onClose },
      )}
      submitError={settleDebt.isError ? t('error.action_failed') : null}
    />
  )
}
