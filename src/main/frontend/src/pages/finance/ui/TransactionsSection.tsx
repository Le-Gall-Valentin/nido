import { useTranslation } from 'react-i18next'
import { Pencil, Repeat, Trash2 } from 'lucide-react'
import { UserAvatar } from '@/entities/user'
import type { SpaceMember } from '@/entities/space'
import type { Category, Transaction } from '@/entities/finance'
import { CategoryIconBadge } from './CategoryIconBadge'
import { formatAmount } from '../lib/formatAmount'

interface TransactionsSectionProps {
  transactions: Transaction[]
  categoryById: Map<string, Category>
  members: SpaceMember[]
  canWrite: boolean
  onManageRecurring: () => void
  onSelectTransaction: (transaction: Transaction) => void
  onEdit: (transaction: Transaction) => void
  onDelete: (transaction: Transaction) => void
}

export function TransactionsSection({
  transactions, categoryById, members, canWrite, onManageRecurring, onSelectTransaction, onEdit, onDelete,
}: TransactionsSectionProps) {
  const { t } = useTranslation('finance')

  return (
    <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-x-2 gap-y-1">
        <h2 className="text-[15px] font-semibold text-fg-0">{t('transactions.title')}</h2>
        {canWrite && (
          <button type="button" onClick={onManageRecurring} className="shrink-0 text-sm font-semibold text-accent">
            {t('recurring_series.manage')}
          </button>
        )}
      </div>
      <ul className="divide-y divide-border">
        {transactions.map((transaction) => {
          const category = categoryById.get(transaction.categoryId)
          const payer = members.find((m) => m.userId === transaction.payerId)
          return (
            <li key={transaction.id} className="flex flex-col gap-1.5 py-3 sm:flex-row sm:items-center sm:gap-3">
              <button type="button" onClick={() => onSelectTransaction(transaction)}
                className="flex min-w-0 flex-1 items-center gap-3 rounded-lg text-left transition-colors hover:bg-bg-2">
                <CategoryIconBadge category={category} />
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-1.5 truncate text-sm font-medium text-fg-0">
                    {transaction.label}
                    {transaction.recurring && (
                      <span className="flex items-center gap-0.5 rounded-full bg-bg-2 px-1.5 py-0.5 text-[10.5px] font-semibold text-fg-3">
                        <Repeat size={10} /> {t('transactions.recurring')}
                      </span>
                    )}
                  </p>
                  <p className="truncate text-xs text-fg-3">{category?.label} · {transaction.date}</p>
                </div>
                {payer && <UserAvatar username={payer.username ?? '?'} role="USER" className="size-6 shrink-0 rounded-full text-[10px]" />}
                <span className={`shrink-0 text-sm font-semibold ${transaction.type === 'EXPENSE' ? 'text-fg-0' : 'text-status-green'}`}>
                  {transaction.type === 'EXPENSE' ? '-' : '+'}{formatAmount(transaction.amount)}
                </span>
              </button>
              {canWrite && (
                <span className="flex shrink-0 items-center justify-end gap-0.5 self-end sm:self-auto">
                  <button type="button" aria-label={t('transactions.edit')} onClick={() => onEdit(transaction)}
                    className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-bg-2 hover:text-fg-1">
                    <Pencil size={15} />
                  </button>
                  <button type="button" aria-label={t('transactions.delete')} onClick={() => onDelete(transaction)}
                    className="grid size-7 place-items-center rounded-md text-fg-3 hover:bg-status-red-dim hover:text-status-red">
                    <Trash2 size={15} />
                  </button>
                </span>
              )}
            </li>
          )
        })}
      </ul>
    </section>
  )
}
