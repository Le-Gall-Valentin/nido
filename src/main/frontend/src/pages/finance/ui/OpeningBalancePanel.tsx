import { useTranslation } from 'react-i18next'
import { useOpeningBalance, useSetOpeningBalance, useClearOpeningBalance } from '@/entities/finance'
import { OpeningBalanceModal } from './OpeningBalanceModal'

interface OpeningBalancePanelProps {
  spaceId: string
  spaceTimezone: string | undefined
  onClose: () => void
}

/** Reading, setting and removing the opening balance: the balance of every month is computed from it. */
export function OpeningBalancePanel({ spaceId, spaceTimezone, onClose }: OpeningBalancePanelProps) {
  const { t } = useTranslation('finance')
  const { data: openingBalance, isPending } = useOpeningBalance(spaceId)
  const setOpeningBalance = useSetOpeningBalance(spaceId)
  const clearOpeningBalance = useClearOpeningBalance(spaceId)

  // Opened before the current one has loaded, the form would start empty and offer no way to remove it.
  if (isPending) return null

  return (
    <OpeningBalanceModal
      openingBalance={openingBalance ?? null}
      spaceTimezone={spaceTimezone}
      onSave={(amount, asOf) => setOpeningBalance.mutate({ amount, asOf }, { onSuccess: onClose })}
      onClear={() => clearOpeningBalance.mutate(undefined, { onSuccess: onClose })}
      onClose={onClose}
      submitError={setOpeningBalance.isError || clearOpeningBalance.isError ? t('form.submit_error') : null}
    />
  )
}
