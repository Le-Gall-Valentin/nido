import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { TransactionsSection } from './TransactionsSection'
import type { Category, Transaction } from '@/entities/finance'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const alimentation: Category = { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }
const categoryById = new Map([['c1', alimentation]])
const alice: SpaceMember = { userId: 'alice', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }

const courses: Transaction = {
  id: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15',
  payerId: 'alice', contributors: [], recurring: false,
}

describe('TransactionsSection', () => {
  it('requests the recurring-series manager and a transaction detail through their respective actions', () => {
    const onManageRecurring = vi.fn()
    const onSelectTransaction = vi.fn()
    render(
      <TransactionsSection transactions={[courses]} categoryById={categoryById} members={[alice]} canWrite
        onManageRecurring={onManageRecurring} onSelectTransaction={onSelectTransaction} onEdit={vi.fn()} onDelete={vi.fn()} />
    )

    fireEvent.click(screen.getByText('recurring_series.manage'))
    fireEvent.click(screen.getByText('Courses'))

    expect(onManageRecurring).toHaveBeenCalled()
    expect(onSelectTransaction).toHaveBeenCalledWith(courses)
  })

  it('shows a recurring badge only for a transaction that belongs to a series', () => {
    const recurring = { ...courses, recurring: true }
    render(
      <TransactionsSection transactions={[recurring]} categoryById={categoryById} members={[alice]} canWrite
        onManageRecurring={vi.fn()} onSelectTransaction={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} />
    )

    expect(screen.getByText('transactions.recurring')).toBeDefined()
  })

  it('edits and deletes a transaction through their respective actions', () => {
    const onEdit = vi.fn()
    const onDelete = vi.fn()
    render(
      <TransactionsSection transactions={[courses]} categoryById={categoryById} members={[alice]} canWrite
        onManageRecurring={vi.fn()} onSelectTransaction={vi.fn()} onEdit={onEdit} onDelete={onDelete} />
    )

    fireEvent.click(screen.getByLabelText('transactions.edit'))
    fireEvent.click(screen.getByLabelText('transactions.delete'))

    expect(onEdit).toHaveBeenCalledWith(courses)
    expect(onDelete).toHaveBeenCalledWith(courses)
  })

  it('hides the recurring-series manager link and every row action from a read-only viewer', () => {
    render(
      <TransactionsSection transactions={[courses]} categoryById={categoryById} members={[alice]} canWrite={false}
        onManageRecurring={vi.fn()} onSelectTransaction={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} />
    )

    expect(screen.queryByText('recurring_series.manage')).toBeNull()
    expect(screen.queryByLabelText('transactions.edit')).toBeNull()
    expect(screen.queryByLabelText('transactions.delete')).toBeNull()
  })
})
