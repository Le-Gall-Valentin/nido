import { useTranslation } from 'react-i18next'
import { Dialog } from '@/shared/ui'
import { UserAvatar } from '@/entities/user'
import type { Transaction } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

interface MemberTransactionsModalProps {
  memberLabel: string
  transactions: Transaction[]
  onSelectTransaction: (transaction: Transaction) => void
  onClose: () => void
}

export function MemberTransactionsModal({ memberLabel, transactions, onSelectTransaction, onClose }: MemberTransactionsModalProps) {
  const { t } = useTranslation('finance')

  return (
    <Dialog open onClose={onClose} title={memberLabel} maxWidth="max-w-lg">
      <div className="mb-4 flex items-center gap-3">
        <UserAvatar username={memberLabel} role="USER" className="size-9 shrink-0 rounded-full text-sm" />
        <h3 className="text-[17px] font-semibold text-fg-0">{memberLabel}</h3>
      </div>

      {transactions.length === 0 ? (
        <p className="text-sm text-fg-3">{t('balances.member_payments_empty')}</p>
      ) : (
        <ul className="max-h-96 divide-y divide-border overflow-y-auto">
          {transactions.map((transaction) => (
            <li key={transaction.id}>
              <button type="button" onClick={() => onSelectTransaction(transaction)}
                className="flex w-full items-center gap-3 rounded-lg py-2 text-left transition-colors hover:bg-bg-2">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-fg-0">{transaction.label}</p>
                  <p className="truncate text-xs text-fg-3">{transaction.date}</p>
                </div>
                <span className={`shrink-0 text-sm font-semibold ${transaction.type === 'EXPENSE' ? 'text-fg-0' : 'text-status-green'}`}>
                  {transaction.type === 'EXPENSE' ? '-' : '+'}{formatAmount(transaction.amount)}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </Dialog>
  )
}
