import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SettleDebtModal } from './SettleDebtModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('SettleDebtModal', () => {
  it('confirms the settlement with the prefilled amount and today by default', () => {
    const onConfirm = vi.fn()
    render(<SettleDebtModal fromLabel="Bob" toLabel="Alice" amount={20} onConfirm={onConfirm} onCancel={vi.fn()} isPending={false} />)

    expect(screen.getByText(/Bob/)).toBeDefined()
    expect(screen.getByText(/Alice/)).toBeDefined()
    fireEvent.click(screen.getByText('balances.settle_confirm'))

    expect(onConfirm).toHaveBeenCalledWith(expect.any(String))
  })
})
