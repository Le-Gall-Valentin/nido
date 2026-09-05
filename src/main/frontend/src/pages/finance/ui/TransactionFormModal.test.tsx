import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { TransactionFormModal } from './TransactionFormModal'
import type { Category } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const categories: Category[] = [{ id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }]

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
})
