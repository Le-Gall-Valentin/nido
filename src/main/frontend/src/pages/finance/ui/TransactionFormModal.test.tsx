import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { TransactionFormModal } from './TransactionFormModal'
import type { Category } from '@/entities/finance'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const categories: Category[] = [
  { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true, type: 'EXPENSE' },
  { id: 'c2', label: 'Revenu', color: '#22c55e', icon: 'Wallet', isDefault: true, type: 'INCOME' },
]

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
  it('defaults to a category matching the initial type even when it is not first in the list', () => {
    const onSubmit = vi.fn()
    // Category order isn't guaranteed by the backend — reproduces a real case where an
    // INCOME category happened to come back before any EXPENSE one.
    const reordered: Category[] = [categories[1], categories[0]]
    renderModal({ onSubmit, categories: reordered })

    const select = screen.getByLabelText('form.category_label') as HTMLSelectElement
    expect(select.value).toBe('c1')

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Courses' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '10' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ type: 'EXPENSE', categoryId: 'c1' }))
  })

  it('only offers categories matching the selected type, and resets the selection when the type changes', () => {
    renderModal()

    const select = screen.getByLabelText('form.category_label') as HTMLSelectElement
    expect(Array.from(select.options).map((o) => o.value)).toEqual(['c1'])

    fireEvent.click(screen.getByText('type.INCOME'))

    expect(Array.from(select.options).map((o) => o.value)).toEqual(['c2'])
    expect(select.value).toBe('c2')
  })

  it('shows the submit error handed down by the caller when the backend rejected the request', () => {
    renderModal({ submitError: 'form.submit_error' })

    expect(screen.getByText('form.submit_error')).toBeDefined()
  })

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

  it('creates a recurring series with no end date by default', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Loyer' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '800' } })
    fireEvent.change(screen.getByLabelText('form.category_label'), { target: { value: 'c1' } })
    fireEvent.click(screen.getByLabelText('form.recurring_label'))
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      recurrence: expect.objectContaining({ endDate: null }),
    }))
  })

  it('creates a recurring series with an end date, for a fixed-term loan', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Prêt voiture' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '250' } })
    fireEvent.change(screen.getByLabelText('form.category_label'), { target: { value: 'c1' } })
    fireEvent.click(screen.getByLabelText('form.recurring_label'))
    fireEvent.change(screen.getByLabelText('recurring_series.end_date_label'), { target: { value: '2027-06-01' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      recurrence: expect.objectContaining({ endDate: '2027-06-01' }),
    }))
  })

  it('rejects an end date that comes before the start date', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Prêt voiture' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '250' } })
    fireEvent.change(screen.getByLabelText('form.category_label'), { target: { value: 'c1' } })
    fireEvent.change(screen.getByLabelText('form.date_label'), { target: { value: '2026-06-01' } })
    fireEvent.click(screen.getByLabelText('form.recurring_label'))
    fireEvent.change(screen.getByLabelText('recurring_series.end_date_label'), { target: { value: '2026-01-01' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('recurring_series.end_date_before_start')).toBeDefined()
  })
})
