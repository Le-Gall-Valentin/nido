import { useTranslation } from 'react-i18next'
import { Repeat } from 'lucide-react'
import { Dialog, Button } from '@/shared/ui'
import { UserAvatar } from '@/entities/user'
import type { SpaceMember } from '@/entities/space'
import type { Category, Transaction } from '@/entities/finance'
import { CategoryIconBadge } from './CategoryIconBadge'
import { formatAmount } from '../lib/formatAmount'
import { roundSharePercentages } from '../lib/roundSharePercentages'

interface TransactionDetailModalProps {
  transaction: Transaction
  category?: Category
  members: SpaceMember[]
  onClose: () => void
}

export function TransactionDetailModal({ transaction, category, members, onClose }: TransactionDetailModalProps) {
  const { t } = useTranslation('finance')
  const payer = members.find((m) => m.userId === transaction.payerId)

  function memberLabel(memberId: string): string {
    const member = members.find((m) => m.userId === memberId)
    return member?.username ?? member?.email ?? memberId
  }

  return (
    <Dialog open onClose={onClose} title={t('transactions.detail_title')} maxWidth="max-w-lg">
      <div className="mb-4 flex items-center gap-3">
        <CategoryIconBadge category={category} size={44} />
        <div className="min-w-0 flex-1">
          <p className="flex items-center gap-1.5 truncate text-[17px] font-semibold text-fg-0">
            {transaction.label}
            {transaction.recurring && (
              <span className="flex items-center gap-0.5 rounded-full bg-bg-2 px-1.5 py-0.5 text-[10.5px] font-semibold text-fg-3">
                <Repeat size={10} /> {t('transactions.recurring')}
              </span>
            )}
          </p>
          <p className="truncate text-sm text-fg-3">{category?.label ?? transaction.categoryId}</p>
        </div>
        <span className={`shrink-0 text-lg font-semibold ${transaction.type === 'EXPENSE' ? 'text-fg-0' : 'text-status-green'}`}>
          {transaction.type === 'EXPENSE' ? '-' : '+'}{formatAmount(transaction.amount)}
        </span>
      </div>

      <dl className="space-y-2.5 text-sm">
        <div className="flex items-center justify-between">
          <dt className="text-fg-3">{t('form.date_label')}</dt>
          <dd className="font-medium text-fg-0">{transaction.date}</dd>
        </div>
        {payer && (
          <div className="flex items-center justify-between">
            <dt className="text-fg-3">{t('form.payer_label')}</dt>
            <dd className="flex items-center gap-1.5 font-medium text-fg-0">
              <UserAvatar username={payer.username ?? '?'} role="USER" className="size-5 rounded-full text-[9px]" />
              {payer.username ?? payer.email}
            </dd>
          </div>
        )}
      </dl>

      {transaction.contributors.length > 0 && (() => {
        const percents = roundSharePercentages(transaction.contributors.map((c) => c.shareAmount), transaction.amount)
        return (
          <div className="mt-4 border-t border-border pt-4">
            <h3 className="mb-2 text-[13px] font-semibold text-fg-1">{t('form.contributors_label')}</h3>
            <ul className="space-y-2">
              {transaction.contributors.map((contribution, i) => (
                <li key={contribution.memberId} className="flex items-center gap-2 text-sm">
                  <UserAvatar username={memberLabel(contribution.memberId)} role="USER" className="size-6 shrink-0 rounded-full text-[10px]" />
                  <span className="flex-1 truncate text-fg-1">{memberLabel(contribution.memberId)}</span>
                  <span className="text-fg-3">{percents[i]}%</span>
                  <span className="w-20 text-right font-medium text-fg-0">{formatAmount(contribution.shareAmount)}</span>
                </li>
              ))}
            </ul>
          </div>
        )
      })()}

      <div className="mt-5 flex justify-end">
        <Button type="button" onClick={onClose}>{t('transactions.close')}</Button>
      </div>
    </Dialog>
  )
}
