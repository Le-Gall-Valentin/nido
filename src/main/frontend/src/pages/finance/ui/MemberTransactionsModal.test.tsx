import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemberTransactionsModal } from './MemberTransactionsModal'
import type { Transaction } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const salaire: Transaction = {
  id: 't1', label: 'Salaire', amount: 1000, type: 'INCOME', categoryId: 'c1', date: '2026-01-05',
  payerId: 'alice', contributors: [], recurring: false,
}
const courses: Transaction = {
  id: 't2', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15',
  payerId: 'alice', contributors: [], recurring: false,
}

describe('MemberTransactionsModal', () => {
  it('lists every transaction paid by the member', () => {
    render(<MemberTransactionsModal memberLabel="alice" transactions={[salaire, courses]} onSelectTransaction={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('Salaire')).toBeDefined()
    expect(screen.getByText('Courses')).toBeDefined()
  })

  it('shows an empty state when the member paid nothing this month', () => {
    render(<MemberTransactionsModal memberLabel="alice" transactions={[]} onSelectTransaction={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('balances.member_payments_empty')).toBeDefined()
  })

  it('requests the detail of the clicked transaction', () => {
    const onSelectTransaction = vi.fn()
    render(<MemberTransactionsModal memberLabel="alice" transactions={[courses]} onSelectTransaction={onSelectTransaction} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('Courses'))

    expect(onSelectTransaction).toHaveBeenCalledWith(courses)
  })
})
