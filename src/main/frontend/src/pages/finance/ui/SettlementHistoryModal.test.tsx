import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { SettlementHistoryModal } from './SettlementHistoryModal'
import type { SettlementRecord } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

function memberLabel(memberId: string): string {
  return memberId === 'alice' ? 'alice' : 'bob'
}

const settlements: SettlementRecord[] = [
  { id: 's1', fromMemberId: 'bob', toMemberId: 'alice', amount: 20, date: '2026-01-02' },
  { id: 's2', fromMemberId: 'alice', toMemberId: 'bob', amount: 15, date: '2026-02-01' },
]

describe('SettlementHistoryModal', () => {
  it('lists every settlement between the two members with direction and amount', () => {
    render(<SettlementHistoryModal settlements={settlements} memberLabel={memberLabel} onClose={vi.fn()} />)

    expect(screen.getByText(/20,00/)).toBeDefined()
    expect(screen.getByText(/15,00/)).toBeDefined()
    expect(screen.getByText('bob → alice')).toBeDefined()
    expect(screen.getByText('alice → bob')).toBeDefined()
  })

  it('shows an empty state when there is no settlement history', () => {
    render(<SettlementHistoryModal settlements={[]} memberLabel={memberLabel} onClose={vi.fn()} />)

    expect(screen.getByText('balances.history_empty')).toBeDefined()
  })
})
