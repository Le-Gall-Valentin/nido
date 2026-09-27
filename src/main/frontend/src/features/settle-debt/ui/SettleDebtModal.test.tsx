import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { formatAmount } from '@/shared/lib'
import { SettleDebtModal } from './SettleDebtModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('SettleDebtModal', () => {
  it('states the debt as an amount of money, not as a raw number', () => {
    render(<SettleDebtModal fromLabel="Bob" toLabel="Alice" amount={42.5} onConfirm={vi.fn()} onCancel={vi.fn()} isPending={false} />)

    // Testing Library collapses the rendered text's non-breaking spaces, not the expected string's.
    const expected = `message:${JSON.stringify({ from: 'Bob', to: 'Alice', amount: formatAmount(42.5) })}`.replace(/\s+/g, ' ')
    expect(screen.getByText(expected)).toBeDefined()
  })

  it('shows the submit error handed down by the caller when the backend rejected the settlement', () => {
    render(<SettleDebtModal fromLabel="Bob" toLabel="Alice" amount={20} onConfirm={vi.fn()} onCancel={vi.fn()} isPending={false} submitError="form.submit_error" />)

    expect(screen.getByText('form.submit_error')).toBeDefined()
  })

  it('confirms the settlement with the prefilled amount and today by default', () => {
    const onConfirm = vi.fn()
    render(<SettleDebtModal fromLabel="Bob" toLabel="Alice" amount={20} onConfirm={onConfirm} onCancel={vi.fn()} isPending={false} />)

    expect(screen.getByText(/Bob/)).toBeDefined()
    expect(screen.getByText(/Alice/)).toBeDefined()
    fireEvent.click(screen.getByText('confirm'))

    expect(onConfirm).toHaveBeenCalledWith(20, expect.any(String))
  })

  it('allows a partial settlement lower than the full debt', () => {
    const onConfirm = vi.fn()
    render(<SettleDebtModal fromLabel="Bob" toLabel="Alice" amount={500} onConfirm={onConfirm} onCancel={vi.fn()} isPending={false} />)

    fireEvent.change(screen.getByLabelText('amount_label'), { target: { value: '250' } })
    fireEvent.click(screen.getByText('confirm'))

    expect(onConfirm).toHaveBeenCalledWith(250, expect.any(String))
  })

  it('rejects an amount greater than the debt', () => {
    const onConfirm = vi.fn()
    render(<SettleDebtModal fromLabel="Bob" toLabel="Alice" amount={500} onConfirm={onConfirm} onCancel={vi.fn()} isPending={false} />)

    fireEvent.change(screen.getByLabelText('amount_label'), { target: { value: '600' } })
    fireEvent.click(screen.getByText('confirm'))

    expect(onConfirm).not.toHaveBeenCalled()
    expect(screen.getByText('amount_too_high')).toBeDefined()
  })

  it('rejects a zero or negative amount', () => {
    const onConfirm = vi.fn()
    render(<SettleDebtModal fromLabel="Bob" toLabel="Alice" amount={500} onConfirm={onConfirm} onCancel={vi.fn()} isPending={false} />)

    fireEvent.change(screen.getByLabelText('amount_label'), { target: { value: '0' } })
    fireEvent.click(screen.getByText('confirm'))

    expect(onConfirm).not.toHaveBeenCalled()
    expect(screen.getByText('amount_required')).toBeDefined()
  })
})
