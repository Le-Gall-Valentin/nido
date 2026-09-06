import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { TransactionDetailModal } from './TransactionDetailModal'
import type { Category, Transaction } from '@/entities/finance'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const alimentation: Category = { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }

const alice: SpaceMember = { userId: 'alice', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }
const bob: SpaceMember = { userId: 'bob', username: 'bob', email: 'b@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }

const transaction: Transaction = {
  id: 't1', label: 'Courses', amount: 100, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15',
  payerId: 'alice', contributors: [{ memberId: 'alice', shareAmount: 60 }, { memberId: 'bob', shareAmount: 40 }],
  recurring: false,
}

describe('TransactionDetailModal', () => {
  it('shows the label, category, amount, date and payer', () => {
    render(<TransactionDetailModal transaction={transaction} category={alimentation} members={[alice, bob]} onClose={vi.fn()} />)

    expect(screen.getByText('Courses')).toBeDefined()
    expect(screen.getByText('Alimentation')).toBeDefined()
    expect(screen.getByText('2026-01-15')).toBeDefined()
    expect(screen.getByText(/100,00/)).toBeDefined()
    expect(screen.getAllByText('alice').length).toBeGreaterThan(0)
  })

  it('shows every contributor with their share amount and percentage of the total', () => {
    render(<TransactionDetailModal transaction={transaction} category={alimentation} members={[alice, bob]} onClose={vi.fn()} />)

    expect(screen.getByText('60%')).toBeDefined()
    expect(screen.getByText(/60,00/)).toBeDefined()
    expect(screen.getByText('40%')).toBeDefined()
    expect(screen.getByText(/40,00/)).toBeDefined()
  })

  it('shows contributor percentages that sum to 100%, even for an uneven three-way split', () => {
    const carol: SpaceMember = { userId: 'carol', username: 'carol', email: 'c@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }
    const threeWay: Transaction = {
      ...transaction,
      amount: 30,
      contributors: [
        { memberId: 'alice', shareAmount: 10 }, { memberId: 'bob', shareAmount: 10 }, { memberId: 'carol', shareAmount: 10 },
      ],
    }
    render(<TransactionDetailModal transaction={threeWay} category={alimentation} members={[alice, bob, carol]} onClose={vi.fn()} />)

    // A naive independent Math.round on each third (33.33%) would show 33%/33%/33% = 99%.
    expect(screen.getByText('34%')).toBeDefined()
    expect(screen.getAllByText('33%')).toHaveLength(2)
  })

  it('shows a recurring badge when the transaction is recurring', () => {
    render(<TransactionDetailModal transaction={{ ...transaction, recurring: true }} category={alimentation} members={[alice, bob]} onClose={vi.fn()} />)

    expect(screen.getByText('transactions.recurring')).toBeDefined()
  })

  it('closes on demand', () => {
    const onClose = vi.fn()
    render(<TransactionDetailModal transaction={transaction} category={alimentation} members={[alice, bob]} onClose={onClose} />)

    fireEvent.click(screen.getByText('transactions.close'))

    expect(onClose).toHaveBeenCalled()
  })
})
