import { useTranslation } from 'react-i18next'
import { Alert, Dialog, Spinner } from '@/shared/ui'
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
  const { data: openingBalance, isPending, isError } = useOpeningBalance(spaceId)
  const setOpeningBalance = useSetOpeningBalance(spaceId)
  const clearOpeningBalance = useClearOpeningBalance(spaceId)

  // Opened before the current one has loaded, or when it could not be, the form would start empty, as if there were
  // none, and offer no way to remove it.
  if (isPending || isError) {
    return (
      <Dialog open onClose={onClose} title={t('opening_balance.title')} maxWidth="max-w-md" showCloseButton>
        {isPending ? <Spinner label={t('loading')} fullscreen={false} /> : <Alert variant="error">{t('opening_balance.load_failed')}</Alert>}
      </Dialog>
    )
  }

  return (
    <OpeningBalanceModal
      openingBalance={openingBalance ?? null}
      spaceTimezone={spaceTimezone}
      onSave={(amount, asOf) => setOpeningBalance.mutate({ amount, asOf }, { onSuccess: onClose })}
      onClear={() => clearOpeningBalance.mutate(undefined, { onSuccess: onClose })}
      onClose={onClose}
      isSaving={setOpeningBalance.isPending || clearOpeningBalance.isPending}
      submitError={setOpeningBalance.isError || clearOpeningBalance.isError ? t('form.submit_error') : null}
    />
  )
}
