import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BalancesSection } from './BalancesSection'
import type { Balances } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

function memberLabel(memberId: string): string {
  return memberId === 'alice' ? 'alice' : 'bob'
}

describe('BalancesSection', () => {
  it('shows every member with what they paid and requests their payments on click', () => {
    const onSelectMember = vi.fn()
    const balances: Balances = { netByMember: [{ memberId: 'alice', net: 20 }], suggestedTransfers: [] }
    render(
      <BalancesSection balances={balances} paidByMember={new Map([['alice', 120.3]])} memberLabel={memberLabel}
        currentUserId="alice" onSelectMember={onSelectMember} onSelectHistory={vi.fn()} onSettle={vi.fn()} />
    )

    expect(screen.getByText(/120,30/)).toBeDefined()
    fireEvent.click(screen.getByText('alice', { exact: false }))

    expect(onSelectMember).toHaveBeenCalledWith('alice')
  })

  it('says everyone is settled up when there is nothing to transfer', () => {
    const balances: Balances = { netByMember: [], suggestedTransfers: [] }
    render(
      <BalancesSection balances={balances} paidByMember={new Map()} memberLabel={memberLabel}
        currentUserId="alice" onSelectMember={vi.fn()} onSelectHistory={vi.fn()} onSettle={vi.fn()} />
    )

    expect(screen.getByText('balances.all_settled')).toBeDefined()
  })

  it('only offers to settle a debt the current user is a party to', () => {
    const balances: Balances = {
      netByMember: [],
      suggestedTransfers: [
        { fromMemberId: 'alice', toMemberId: 'bob', amount: 100 },
        { fromMemberId: 'bob', toMemberId: 'carol', amount: 50 },
      ],
    }
    render(
      <BalancesSection balances={balances} paidByMember={new Map()} memberLabel={memberLabel}
        currentUserId="alice" onSelectMember={vi.fn()} onSelectHistory={vi.fn()} onSettle={vi.fn()} />
    )

    expect(screen.getAllByText('balances.settle')).toHaveLength(1)
  })

  it('opens the settlement history and settles a debt through their respective actions', () => {
    const onSelectHistory = vi.fn()
    const onSettle = vi.fn()
    const transfer = { fromMemberId: 'alice', toMemberId: 'bob', amount: 100 }
    const balances: Balances = { netByMember: [], suggestedTransfers: [transfer] }
    render(
      <BalancesSection balances={balances} paidByMember={new Map()} memberLabel={memberLabel}
        currentUserId="alice" onSelectMember={vi.fn()} onSelectHistory={onSelectHistory} onSettle={onSettle} />
    )

    fireEvent.click(screen.getByText(/→/))
    fireEvent.click(screen.getByText('balances.settle'))

    expect(onSelectHistory).toHaveBeenCalledWith({ memberAId: 'alice', memberBId: 'bob' })
    expect(onSettle).toHaveBeenCalledWith(transfer)
  })
})
