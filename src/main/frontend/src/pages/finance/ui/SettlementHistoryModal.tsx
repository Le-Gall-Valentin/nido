import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import type { SettlementRecord } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

interface SettlementHistoryModalProps {
  settlements: SettlementRecord[]
  memberLabel: (memberId: string) => string
  onClose: () => void
}

export function SettlementHistoryModal({ settlements, memberLabel, onClose }: SettlementHistoryModalProps) {
  const { t } = useTranslation('finance')

  return (
    <Dialog open onClose={onClose} title={t('balances.history_title')} maxWidth="max-w-lg" showCloseButton>
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{t('balances.history_title')}</h3>

      {settlements.length === 0 ? (
        <p className="text-sm text-fg-3">{t('balances.history_empty')}</p>
      ) : (
        <ul className="max-h-96 space-y-2 overflow-y-auto">
          {settlements.map((settlement) => (
            <li key={settlement.id} className="flex items-center justify-between rounded-[10px] bg-bg-2 px-3 py-2 text-sm">
              <span className="text-fg-1">{memberLabel(settlement.fromMemberId)} → {memberLabel(settlement.toMemberId)}</span>
              <span className="flex items-center gap-3">
                <span className="text-fg-3">{settlement.date}</span>
                <span className="font-semibold text-fg-0">{formatAmount(settlement.amount)}</span>
              </span>
            </li>
          ))}
        </ul>
      )}
    </Dialog>
  )
}
