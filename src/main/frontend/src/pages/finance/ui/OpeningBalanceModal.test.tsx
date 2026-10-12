import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { todayIso } from '@/shared/lib'
import { OpeningBalanceModal } from './OpeningBalanceModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const ZONE = 'Europe/Paris'

function renderModal(overrides: Partial<Parameters<typeof OpeningBalanceModal>[0]> = {}) {
  const props = {
    openingBalance: null, spaceTimezone: ZONE, onSave: vi.fn(), onClear: vi.fn(), onClose: vi.fn(), ...overrides,
  }
  render(<OpeningBalanceModal {...props} />)
  return props
}

function amountInput() {
  return screen.getByLabelText('opening_balance.amount_label') as HTMLInputElement
}

function dateInput() {
  return screen.getByLabelText('opening_balance.date_label') as HTMLInputElement
}

describe('OpeningBalanceModal', () => {
  it("opens on today's date, on the household's calendar, with no amount when there is none yet", () => {
    renderModal()

    expect(dateInput().value).toBe(todayIso(new Date(), ZONE))
    expect(amountInput().value).toBe('')
    expect(screen.queryByText('opening_balance.clear')).toBeNull()
  })

  it('opens on the current opening balance, and offers to remove it', () => {
    const props = renderModal({ openingBalance: { amount: 1200, asOf: '2026-09-30' } })

    expect(dateInput().value).toBe('2026-09-30')
    expect(amountInput().value).toBe('1200')
    fireEvent.click(screen.getByText('opening_balance.clear'))
    expect(props.onClear).toHaveBeenCalled()
  })

  it('saves an overdraft', () => {
    const props = renderModal()

    fireEvent.change(amountInput(), { target: { value: '-150.5' } })
    fireEvent.change(dateInput(), { target: { value: '2026-10-01' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(props.onSave).toHaveBeenCalledWith(-150.5, '2026-10-01')
  })

  it('refuses an amount with more than two decimals', () => {
    const props = renderModal()

    fireEvent.change(amountInput(), { target: { value: '12.345' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(screen.getByText('opening_balance.amount_invalid')).toBeDefined()
    expect(props.onSave).not.toHaveBeenCalled()
  })

  it('refuses an empty amount', () => {
    const props = renderModal()

    fireEvent.click(screen.getByText('form.save'))

    expect(screen.getByText('opening_balance.amount_invalid')).toBeDefined()
    expect(props.onSave).not.toHaveBeenCalled()
  })

  it('refuses a date after today', () => {
    const props = renderModal()

    fireEvent.change(amountInput(), { target: { value: '100' } })
    fireEvent.change(dateInput(), { target: { value: '2999-01-01' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(screen.getByText('opening_balance.date_in_future')).toBeDefined()
    expect(props.onSave).not.toHaveBeenCalled()
  })
})
