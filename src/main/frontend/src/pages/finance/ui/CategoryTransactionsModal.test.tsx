import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { CategoryTransactionsModal } from './CategoryTransactionsModal'
import type { Category, Transaction } from '@/entities/finance'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const alimentation: Category = { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true, type: 'EXPENSE' }
const alice: SpaceMember = { userId: 'alice', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }

const courses: Transaction = {
  id: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15',
  payerId: 'alice', contributors: [], recurring: false,
}
const boulangerie: Transaction = {
  id: 't2', label: 'Boulangerie', amount: 5, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-16',
  payerId: 'alice', contributors: [], recurring: false,
}

describe('CategoryTransactionsModal', () => {
  it('lists every transaction for the category', () => {
    render(<CategoryTransactionsModal category={alimentation} transactions={[courses, boulangerie]} members={[alice]} onSelectTransaction={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('Courses')).toBeDefined()
    expect(screen.getByText('Boulangerie')).toBeDefined()
  })

  it('shows an empty state when the category has no transactions this month', () => {
    render(<CategoryTransactionsModal category={alimentation} transactions={[]} members={[alice]} onSelectTransaction={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('transactions.category_empty')).toBeDefined()
  })

  it('requests the detail of the clicked transaction', () => {
    const onSelectTransaction = vi.fn()
    render(<CategoryTransactionsModal category={alimentation} transactions={[courses]} members={[alice]} onSelectTransaction={onSelectTransaction} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('Courses'))

    expect(onSelectTransaction).toHaveBeenCalledWith(courses)
  })
})
