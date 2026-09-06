import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SpendingBreakdownSection } from './SpendingBreakdownSection'
import type { Category, CategoryAmount } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const alimentation: Category = { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }
const categoryById = new Map([['c1', alimentation]])

describe('SpendingBreakdownSection', () => {
  it('shows an empty state when there is no spending', () => {
    render(<SpendingBreakdownSection breakdown={[]} categoryById={categoryById} onSelectCategory={vi.fn()} />)

    expect(screen.getByText('breakdown.empty')).toBeDefined()
  })

  it('lists each category with its share of the total, and requests its transactions on click', () => {
    const onSelectCategory = vi.fn()
    const breakdown: CategoryAmount[] = [{ categoryId: 'c1', amount: 195.3 }]
    render(<SpendingBreakdownSection breakdown={breakdown} categoryById={categoryById} onSelectCategory={onSelectCategory} />)

    expect(screen.getByText('Alimentation')).toBeDefined()
    expect(screen.getByText('100%')).toBeDefined()
    expect(screen.getByText(/195,30/)).toBeDefined()

    fireEvent.click(screen.getByText('Alimentation'))

    expect(onSelectCategory).toHaveBeenCalledWith('c1')
  })

  it('shows a tooltip with the category, percentage and amount when hovering its donut segment', () => {
    const breakdown: CategoryAmount[] = [{ categoryId: 'c1', amount: 195.3 }]
    render(<SpendingBreakdownSection breakdown={breakdown} categoryById={categoryById} onSelectCategory={vi.fn()} />)

    fireEvent.mouseEnter(screen.getByRole('img'), { clientX: 10, clientY: 10 })

    expect(screen.getByRole('tooltip')).toBeDefined()
    expect(screen.getAllByText('Alimentation').length).toBeGreaterThan(1)
    expect(screen.getByText(/100% · 195,30/)).toBeDefined()
  })
})
