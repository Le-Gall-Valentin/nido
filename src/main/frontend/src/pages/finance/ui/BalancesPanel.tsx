import { useState } from 'react'
import {
  useBalances, useSettlementsBetween, type Transaction,
} from '@/entities/finance'
import { BalancesSection } from './BalancesSection'
import { SettleDebtDialog, type DebtToSettle } from '@/features/settle-debt'
import { MemberTransactionsModal } from './MemberTransactionsModal'
import { SettlementHistoryModal } from './SettlementHistoryModal'

interface BalancesPanelProps {
  spaceId: string
  spaceTimezone: string | undefined
  transactions: Transaction[]
  currentUserId: string | null
  memberLabel: (memberId: string) => string
  /** Opening an operation from here hands it back to the page, which stacks that modal above this one. */
  onSelectTransaction: (transaction: Transaction) => void
}

/**
 * Who owes whom, and the three things one can do about it: settle a debt, look at what a member paid,
 * read the history between two of them.
 *
 * <p>The settlement history is why this is a component and not three more slices of page state: its
 * query takes the pair of members being looked at, so it can only be asked for once that pair is known.
 * Held in the page, the hook had to run on every render with two undefined arguments.
 */
export function BalancesPanel({
  spaceId, spaceTimezone, transactions, currentUserId, memberLabel, onSelectTransaction,
}: BalancesPanelProps) {
  const { data: balances } = useBalances(spaceId)

  const [settling, setSettling] = useState<DebtToSettle | null>(null)
  const [viewingMemberId, setViewingMemberId] = useState<string | null>(null)
  const [viewingHistoryBetween, setViewingHistoryBetween] = useState<{ memberAId: string; memberBId: string } | null>(null)

  const { data: settlementsBetween } = useSettlementsBetween(
    spaceId, viewingHistoryBetween?.memberAId, viewingHistoryBetween?.memberBId)

  // Expenses only, on purpose: this is the "paid X" line next to each net balance, and money received
  // is not money paid. A shared income still moves the net figure — the server folds it — it just does
  // not belong in this figure.
  const paidByMember = new Map<string, number>()
  for (const transaction of transactions) {
    if (transaction.type === 'EXPENSE' && transaction.payerId) {
      paidByMember.set(transaction.payerId, (paidByMember.get(transaction.payerId) ?? 0) + transaction.amount)
    }
  }

  return (
    <>
      <BalancesSection
        balances={balances}
        paidByMember={paidByMember}
        memberLabel={memberLabel}
        currentUserId={currentUserId}
        onSelectMember={setViewingMemberId}
        onSelectHistory={setViewingHistoryBetween}
        onSettle={setSettling}
      />

      {settling && (
        <SettleDebtDialog spaceId={spaceId} spaceTimezone={spaceTimezone} debt={settling} memberLabel={memberLabel}
          onClose={() => setSettling(null)} />
      )}

      {viewingMemberId && (
        <MemberTransactionsModal
          memberId={viewingMemberId}
          memberLabel={memberLabel(viewingMemberId)}
          transactions={transactions.filter((transaction) => transaction.payerId === viewingMemberId)}
          onSelectTransaction={onSelectTransaction}
          onClose={() => setViewingMemberId(null)}
        />
      )}

      {viewingHistoryBetween && (
        <SettlementHistoryModal
          settlements={settlementsBetween ?? []}
          memberLabel={memberLabel}
          onClose={() => setViewingHistoryBetween(null)}
        />
      )}
    </>
  )
}
