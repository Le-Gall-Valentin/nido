import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { RecurringSeriesFormModal } from './RecurringSeriesFormModal'
import type { Category, RecurringSeries } from '@/entities/finance'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const categories: Category[] = [{ id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }]
const alice: SpaceMember = { userId: 'alice', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }
const bob: SpaceMember = { userId: 'bob', username: 'bob', email: 'b@test.com', role: 'MEMBER', joinedAt: '2026-01-01' }

const series: RecurringSeries = {
  id: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice',
  contributors: [{ memberId: 'alice', shareAmount: 400 }, { memberId: 'bob', shareAmount: 400 }],
  intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null,
}

function renderModal(props: Partial<React.ComponentProps<typeof RecurringSeriesFormModal>> = {}) {
  return render(
    <RecurringSeriesFormModal
      series={series} categories={categories} members={[alice, bob]} canPickContributors
      onSubmit={vi.fn()} onCancel={vi.fn()} {...props}
    />
  )
}

describe('RecurringSeriesFormModal', () => {
  it('prefills every field from the existing series', () => {
    renderModal()

    expect(screen.getByDisplayValue('Loyer')).toBeDefined()
    expect(screen.getByDisplayValue('800')).toBeDefined()
    expect(screen.getByDisplayValue('2026-01-01')).toBeDefined()
    expect(screen.getByLabelText('form.recurrence_interval_count_label')).toBeDefined()
  })

  it('submits the edited fields', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit })

    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Loyer augmenté' } })
    fireEvent.change(screen.getByLabelText('form.amount_label'), { target: { value: '850' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      label: 'Loyer augmenté', amount: 850, categoryId: 'c1', payerId: 'alice',
      contributors: [{ memberId: 'alice', shareAmount: null }, { memberId: 'bob', shareAmount: null }],
      recurrence: { intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null },
    }))
  })

  it('rejects submitting without a payer when there are contributors', () => {
    const onSubmit = vi.fn()
    renderModal({ onSubmit, series: { ...series, payerId: null } })

    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('form.payer_required')).toBeDefined()
  })
})
