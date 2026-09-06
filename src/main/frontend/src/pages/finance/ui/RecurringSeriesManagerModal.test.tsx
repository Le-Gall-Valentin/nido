import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { RecurringSeriesManagerModal } from './RecurringSeriesManagerModal'
import type { RecurringSeries } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const rent: RecurringSeries = {
  id: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'alice',
  contributors: [], intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null,
}

describe('RecurringSeriesManagerModal', () => {
  it('lists every recurring series', () => {
    render(<RecurringSeriesManagerModal series={[rent]} onEdit={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('Loyer')).toBeDefined()
  })

  it('shows an empty state when there are no recurring series', () => {
    render(<RecurringSeriesManagerModal series={[]} onEdit={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('recurring_series.empty')).toBeDefined()
  })

  it('requests editing the clicked series', () => {
    const onEdit = vi.fn()
    render(<RecurringSeriesManagerModal series={[rent]} onEdit={onEdit} onDelete={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(screen.getByLabelText('recurring_series.edit'))

    expect(onEdit).toHaveBeenCalledWith(rent)
  })

  it('requests deleting the clicked series', () => {
    const onDelete = vi.fn()
    render(<RecurringSeriesManagerModal series={[rent]} onEdit={vi.fn()} onDelete={onDelete} onClose={vi.fn()} />)

    fireEvent.click(screen.getByLabelText('recurring_series.delete'))

    expect(onDelete).toHaveBeenCalledWith('s1')
  })

  it('shows the end date for a fixed-term series', () => {
    const loan: RecurringSeries = { ...rent, id: 's2', label: 'Prêt voiture', endDate: '2026-06-01' }
    render(<RecurringSeriesManagerModal series={[loan]} onEdit={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText(/2026-06-01/)).toBeDefined()
  })

  it('does not show an end date for an ongoing series', () => {
    render(<RecurringSeriesManagerModal series={[rent]} onEdit={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} />)

    expect(screen.queryByText(/recurring_series.end_date_label/)).toBeNull()
  })
})
