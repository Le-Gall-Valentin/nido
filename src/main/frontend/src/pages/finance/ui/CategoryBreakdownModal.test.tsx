import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { CategoryBreakdownModal } from './CategoryBreakdownModal'
import type { Category, CategoryAmount } from '@/entities/finance'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const alimentation: Category = { id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true, type: 'EXPENSE' }
const revenu: Category = { id: 'c2', label: 'Revenu', color: '#22c55e', icon: 'Wallet', isDefault: true, type: 'INCOME' }
const categoryById = new Map([['c1', alimentation], ['c2', revenu]])

describe('CategoryBreakdownModal', () => {
  it('only lists categories matching the given type', () => {
    const breakdown: CategoryAmount[] = [{ categoryId: 'c1', amount: 195.3 }, { categoryId: 'c2', amount: 1000 }]
    render(<CategoryBreakdownModal type="EXPENSE" breakdown={breakdown} categoryById={categoryById} onSelectCategory={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('Alimentation')).toBeDefined()
    expect(screen.getByText(/195,30/)).toBeDefined()
    expect(screen.queryByText('Revenu')).toBeNull()
  })

  it('requests the category transactions when a row is clicked', () => {
    const onSelectCategory = vi.fn()
    const breakdown: CategoryAmount[] = [{ categoryId: 'c2', amount: 1000 }]
    render(<CategoryBreakdownModal type="INCOME" breakdown={breakdown} categoryById={categoryById} onSelectCategory={onSelectCategory} onClose={vi.fn()} />)

    fireEvent.click(screen.getByText('Revenu'))

    expect(onSelectCategory).toHaveBeenCalledWith('c2')
  })

  it('shows an empty state when there is nothing of that type', () => {
    render(<CategoryBreakdownModal type="INCOME" breakdown={[]} categoryById={categoryById} onSelectCategory={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByText('breakdown.empty_INCOME')).toBeDefined()
  })
})
