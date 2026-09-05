import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { TransactionFormModal } from './TransactionFormModal'
import type { Category } from '@/entities/finance'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const categories: Category[] = [{ id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }]

const alice: SpaceMember = { userId: 'alice', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }
const bob: SpaceMember = { userId: 'bob', username: 'bob', email: 'b@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }

function renderModal(props: Partial<React.ComponentProps<typeof TransactionFormModal>> = {}) {
  return render(
    <TransactionFormModal
      mode="create" categories={categories} members={[]} canPickContributors={false}
      onSubmit={vi.fn()} onCancel={vi.fn()} {...props}
    />
  )
}

describe('TransactionFormModal', () => {
  it('rejects submitting without a label', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })

    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('form.label_required')).toBeDefined()
  })

  it('submits the entered fields for a one-off expense', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Courses' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '45.30' } })
    fireEvent.change(screen.getByLabelText('form.category_label'), { target: { value: 'c1' } })
    fireEvent.change(screen.getByLabelText('form.date_label'), { target: { value: '2026-01-15' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15',
      payerId: null, contributors: [], recurrence: null,
    }))
  })

  it('does not render the payer/contributor pickers when canPickContributors is false', () => {
    renderModal({ canPickContributors: false })

    expect(screen.queryByLabelText('form.payer_label')).toBeNull()
  })

  it('defaults the payer to the current user and every member as a contributor in a shared space', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit, canPickContributors: true, members: [alice, bob], currentUserId: 'alice' })

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Courses' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '40.00' } })
    fireEvent.change(screen.getByLabelText('form.category_label'), { target: { value: 'c1' } })
    fireEvent.change(screen.getByLabelText('form.date_label'), { target: { value: '2026-01-15' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      payerId: 'alice',
      contributors: [{ memberId: 'alice', shareAmount: null }, { memberId: 'bob', shareAmount: null }],
    }))
  })

  it('has no "nobody" option for the payer in a shared space', () => {
    renderModal({ canPickContributors: true, members: [alice, bob], currentUserId: 'alice' })

    expect(screen.queryByText('form.payer_none')).toBeNull()
  })

  it('anchors a recurring series to the transaction date, with no separate "starting on" field', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Loyer' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '800' } })
    fireEvent.change(screen.getByLabelText('form.category_label'), { target: { value: 'c1' } })
    fireEvent.change(screen.getByLabelText('form.date_label'), { target: { value: '2026-02-01' } })
    fireEvent.click(screen.getByLabelText('form.recurring_label'))

    expect(screen.queryByText('form.recurrence_anchor_date_label')).toBeNull()

    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      date: '2026-02-01',
      recurrence: expect.objectContaining({ anchorDate: '2026-02-01', intervalType: 'MONTHLY', intervalCount: 1 }),
    }))
  })

  it('offers a yearly recurrence interval', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Assurance' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '250' } })
    fireEvent.change(screen.getByLabelText('form.category_label'), { target: { value: 'c1' } })
    fireEvent.click(screen.getByLabelText('form.recurring_label'))
    fireEvent.change(screen.getByLabelText('form.recurrence_interval_type_label'), { target: { value: 'YEARLY' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      recurrence: expect.objectContaining({ intervalType: 'YEARLY' }),
    }))
  })
})
