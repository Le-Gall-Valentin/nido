import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, ConfirmDeleteModal, Dialog, Spinner } from '@/shared/ui'
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
  // Removing it changes the balance of every month, and getting it back means finding that day's bank balance again.
  const [confirmingClear, setConfirmingClear] = useState(false)

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
    <>
      <OpeningBalanceModal
        openingBalance={openingBalance ?? null}
        spaceTimezone={spaceTimezone}
        onSave={(amount, asOf) => setOpeningBalance.mutate({ amount, asOf }, { onSuccess: onClose })}
        onClear={() => setConfirmingClear(true)}
        onClose={onClose}
        isSaving={setOpeningBalance.isPending || clearOpeningBalance.isPending}
        submitError={setOpeningBalance.isError ? t('form.submit_error') : null}
      />

      {confirmingClear && (
        <ConfirmDeleteModal
          title={t('opening_balance.clear_title')}
          message={t('opening_balance.clear_message')}
          confirmLabel={t('opening_balance.clear_confirm')}
          isPending={clearOpeningBalance.isPending}
          error={clearOpeningBalance.isError ? t('form.submit_error') : null}
          onCancel={() => {
            setConfirmingClear(false)
            clearOpeningBalance.reset()
          }}
          onConfirm={() => clearOpeningBalance.mutate(undefined, { onSuccess: onClose })}
        />
      )}
    </>
  )
}
