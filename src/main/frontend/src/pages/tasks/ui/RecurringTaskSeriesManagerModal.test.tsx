import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { RecurringTaskSeries } from '@/entities/tasks'
import { RecurringTaskSeriesManagerModal } from './RecurringTaskSeriesManagerModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const SERIES: RecurringTaskSeries[] = [
  { id: 's-1', title: 'Sortir les poubelles', priority: 'MED', subtaskTemplates: [],
    intervalType: 'WEEKLY', intervalCount: 1, leadIntervalType: 'DAILY', leadIntervalCount: 2,
    anchorDate: '2026-01-07', endDate: null, rotationMemberIds: [] },
  { id: 's-2', title: 'Payer le loyer', priority: 'HIGH', subtaskTemplates: [],
    intervalType: 'MONTHLY', intervalCount: 1, leadIntervalType: 'WEEKLY', leadIntervalCount: 1,
    anchorDate: '2026-01-01', endDate: '2027-01-01', rotationMemberIds: [] },
]

describe('RecurringTaskSeriesManagerModal', () => {
  it('shows the empty state when there is no series', () => {
    render(<RecurringTaskSeriesManagerModal series={[]} onEdit={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('recurring_series.empty')).toBeDefined()
  })

  it('lists every series with its title', () => {
    render(<RecurringTaskSeriesManagerModal series={SERIES} onEdit={vi.fn()} onDelete={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('Sortir les poubelles')).toBeDefined()
    expect(screen.getByText('Payer le loyer')).toBeDefined()
  })

  it('calls onEdit with the clicked series', () => {
    const onEdit = vi.fn()
    render(<RecurringTaskSeriesManagerModal series={SERIES} onEdit={onEdit} onDelete={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(screen.getAllByLabelText('recurring_series.edit')[0])

    expect(onEdit).toHaveBeenCalledWith(SERIES[0])
  })

  it('calls onDelete with the clicked series id', () => {
    const onDelete = vi.fn()
    render(<RecurringTaskSeriesManagerModal series={SERIES} onEdit={vi.fn()} onDelete={onDelete} onClose={vi.fn()} />)

    fireEvent.click(screen.getAllByLabelText('recurring_series.delete')[1])

    expect(onDelete).toHaveBeenCalledWith('s-2')
  })
})
