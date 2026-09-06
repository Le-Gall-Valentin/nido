import { useTranslation } from 'react-i18next'
import { ArrowRight, CheckCircle2, HandCoins } from 'lucide-react'
import { UserAvatar } from '@/entities/user'
import type { Balances, SuggestedTransfer } from '@/entities/finance'
import { formatAmount } from '../lib/formatAmount'

interface BalancesSectionProps {
  balances?: Balances
  paidByMember: Map<string, number>
  memberLabel: (memberId: string) => string
  currentUserId: string | null
  onSelectMember: (memberId: string) => void
  onSelectHistory: (between: { memberAId: string; memberBId: string }) => void
  onSettle: (transfer: SuggestedTransfer) => void
}

export function BalancesSection({
  balances, paidByMember, memberLabel, currentUserId, onSelectMember, onSelectHistory, onSettle,
}: BalancesSectionProps) {
  const { t } = useTranslation('finance')

  return (
    <section className="mt-4 rounded-2xl border border-border bg-bg-1 p-4">
      <h2 className="flex items-center gap-1.5 text-[15px] font-semibold text-fg-0">
        <HandCoins size={16} className="text-fg-3" /> {t('balances.title')}
      </h2>
      <p className="mb-4 text-[13px] text-fg-3">{t('balances.subtitle')}</p>
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6">
        <ul className="space-y-1">
          {balances?.netByMember.map((row) => {
            const paid = paidByMember.get(row.memberId) ?? 0
            const statusKey = row.net >= 0.01 ? 'status_owed' : row.net <= -0.01 ? 'status_owes' : 'status_settled'
            const netColor = row.net >= 0.01 ? 'text-status-green' : row.net <= -0.01 ? 'text-status-red' : 'text-fg-3'
            return (
              <li key={row.memberId}>
                <button type="button" onClick={() => onSelectMember(row.memberId)}
                  className="flex w-full items-center gap-2.5 rounded-lg p-1.5 text-left transition-colors hover:bg-bg-2">
                  <UserAvatar username={memberLabel(row.memberId)} role="USER" className="size-8 shrink-0 rounded-full text-xs" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[13.5px] font-medium text-fg-0">
                      {memberLabel(row.memberId)} <span className="font-normal text-fg-3">· {t('balances.paid_prefix', { amount: formatAmount(paid) })}</span>
                    </p>
                    <p className="text-xs text-fg-3">{t(`balances.${statusKey}`)}</p>
                  </div>
                  <span className={`shrink-0 text-[14.5px] font-semibold ${netColor}`}>
                    {row.net > 0 ? '+ ' : row.net < 0 ? '– ' : ''}{formatAmount(Math.abs(row.net))}
                  </span>
                </button>
              </li>
            )
          })}
        </ul>

        <div className="flex flex-col gap-2 sm:border-l sm:border-border sm:pl-6">
          {(balances?.suggestedTransfers.length ?? 0) === 0 ? (
            <div className="flex h-full min-h-20 items-center justify-center gap-2 text-sm text-fg-3">
              <CheckCircle2 size={18} className="text-status-green" /> {t('balances.all_settled')}
            </div>
          ) : (
            balances?.suggestedTransfers.map((transfer) => (
              <div key={`${transfer.fromMemberId}-${transfer.toMemberId}`} className="flex flex-col gap-2 rounded-[11px] bg-bg-2 p-2.5 sm:flex-row sm:items-center sm:gap-2.5">
                <div className="flex items-center gap-2.5 sm:contents">
                  <UserAvatar username={memberLabel(transfer.fromMemberId)} role="USER" className="size-7 shrink-0 rounded-full text-[10.5px]" />
                  <ArrowRight size={16} className="shrink-0 text-fg-3" />
                  <UserAvatar username={memberLabel(transfer.toMemberId)} role="USER" className="size-7 shrink-0 rounded-full text-[10.5px]" />
                  <button type="button"
                    onClick={() => onSelectHistory({ memberAId: transfer.fromMemberId, memberBId: transfer.toMemberId })}
                    className="min-w-0 flex-1 truncate rounded text-left text-sm text-fg-1 hover:underline">
                    {memberLabel(transfer.fromMemberId)} → {memberLabel(transfer.toMemberId)}
                  </button>
                </div>
                <div className="flex items-center justify-between gap-2 sm:contents">
                  <span className="shrink-0 text-sm font-semibold text-fg-0">{formatAmount(transfer.amount)}</span>
                  {(transfer.fromMemberId === currentUserId || transfer.toMemberId === currentUserId) && (
                    <button type="button" onClick={() => onSettle(transfer)}
                      className="shrink-0 rounded-[8px] bg-accent px-2.5 py-1 text-xs font-semibold text-white">{t('balances.settle')}</button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </section>
  )
}
