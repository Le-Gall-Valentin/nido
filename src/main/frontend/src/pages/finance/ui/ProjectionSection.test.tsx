import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ProjectionSection } from './ProjectionSection'
import type { Projection } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('ProjectionSection', () => {
  it('shows the projected end-of-month balance and every upcoming occurrence', () => {
    const projection: Projection = {
      actualBalanceSoFar: -50,
      upcoming: [{ seriesId: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', date: '2026-01-28' }],
      projectedEndOfMonthBalance: -850,
    }
    render(<ProjectionSection projection={projection} />)

    expect(screen.getByText(/-850,00/)).toBeDefined()
    expect(screen.getByText(/Loyer/)).toBeDefined()
  })

  it('does not render an upcoming list when there is nothing scheduled', () => {
    const projection: Projection = { actualBalanceSoFar: 0, upcoming: [], projectedEndOfMonthBalance: 0 }
    render(<ProjectionSection projection={projection} />)

    expect(screen.queryByRole('list')).toBeNull()
  })
})
