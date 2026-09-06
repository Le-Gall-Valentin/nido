import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { FinanceHeader } from './FinanceHeader'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('FinanceHeader', () => {
  it('reports the selected month when changed', () => {
    const onMonthChange = vi.fn()
    render(
      <FinanceHeader month="2026-01" onMonthChange={onMonthChange} canWrite
        onManageCategories={vi.fn()} onNewTransaction={vi.fn()} />
    )

    fireEvent.change(screen.getByDisplayValue('2026-01'), { target: { value: '2026-02' } })

    expect(onMonthChange).toHaveBeenCalledWith('2026-02')
  })

  it('requests to manage categories and to create a transaction when a writer clicks the buttons', () => {
    const onManageCategories = vi.fn()
    const onNewTransaction = vi.fn()
    render(
      <FinanceHeader month="2026-01" onMonthChange={vi.fn()} canWrite
        onManageCategories={onManageCategories} onNewTransaction={onNewTransaction} />
    )

    fireEvent.click(screen.getByText('categories.manage'))
    fireEvent.click(screen.getByText('new_transaction'))

    expect(onManageCategories).toHaveBeenCalled()
    expect(onNewTransaction).toHaveBeenCalled()
  })

  it('hides the manage-categories and new-transaction buttons from a read-only viewer', () => {
    render(
      <FinanceHeader month="2026-01" onMonthChange={vi.fn()} canWrite={false}
        onManageCategories={vi.fn()} onNewTransaction={vi.fn()} />
    )

    expect(screen.queryByText('categories.manage')).toBeNull()
    expect(screen.queryByText('new_transaction')).toBeNull()
  })
})
