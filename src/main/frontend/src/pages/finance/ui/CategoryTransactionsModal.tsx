import { useTranslation } from 'react-i18next'
import { Dialog, Tooltip } from '@/shared/ui'
import { UserAvatar } from '@/entities/user'
import { useMemberName, type SpaceMember } from '@/entities/space'
import type { Category, Transaction } from '@/entities/finance'
import { CategoryIconBadge } from './CategoryIconBadge'
import { formatAmount } from '@/shared/lib'

interface CategoryTransactionsModalProps {
  category?: Category
  transactions: Transaction[]
  members: SpaceMember[]
  onSelectTransaction: (transaction: Transaction) => void
  onClose: () => void
}

export function CategoryTransactionsModal({ category, transactions, members, onSelectTransaction, onClose }: CategoryTransactionsModalProps) {
  const { t } = useTranslation('finance')
  const memberName = useMemberName(members)

  return (
    <Dialog open onClose={onClose} title={category?.label ?? t('transactions.title')} maxWidth="max-w-lg" showCloseButton>
      <div className="mb-4 flex items-center gap-3 pr-8">
        <CategoryIconBadge category={category} size={38} />
        <h3 className="min-w-0 truncate text-[17px] font-semibold text-fg-0">{category?.label ?? t('transactions.title')}</h3>
      </div>

      {transactions.length === 0 ? (
        <p className="text-sm text-fg-3">{t('transactions.category_empty')}</p>
      ) : (
        <ul className="max-h-96 divide-y divide-border overflow-y-auto">
          {transactions.map((transaction) => {
            const payer = members.find((m) => m.userId === transaction.payerId)
            return (
              <li key={transaction.id}>
                <button type="button" onClick={() => onSelectTransaction(transaction)}
                  className="flex w-full items-center gap-3 rounded-lg py-2 text-left transition-colors hover:bg-bg-2">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-fg-0">{transaction.label}</p>
                    <p className="truncate text-xs text-fg-3">{transaction.date}</p>
                  </div>
                  {payer && (
                    <Tooltip label={memberName(payer.userId)} className="shrink-0">
                      <UserAvatar userId={payer.userId} username={memberName(payer.userId)} className="size-6 rounded-full text-[10px]" />
                    </Tooltip>
                  )}
                  <span className={`shrink-0 text-sm font-semibold ${transaction.type === 'EXPENSE' ? 'text-fg-0' : 'text-status-green'}`}>
                    {transaction.type === 'EXPENSE' ? '-' : '+'}{formatAmount(transaction.amount)}
                  </span>
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </Dialog>
  )
}
