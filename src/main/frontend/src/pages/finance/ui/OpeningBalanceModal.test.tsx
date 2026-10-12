import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
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
  afterEach(() => {
    vi.useRealTimers()
  })

  it("opens on today's date on the household's calendar, not the browser's, with no amount when there is none yet", () => {
    // 22:30 in UTC is already the 12th in Paris: a form that read the browser's calendar would open on the 11th.
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-11T22:30:00Z'))
    renderModal()

    expect(dateInput().value).toBe('2026-10-12')
    expect(amountInput().value).toBe('')
    expect(screen.queryByText('opening_balance.clear')).toBeNull()
  })

  it("accepts the household's today and refuses its tomorrow, whatever the browser's calendar says", () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-11T22:30:00Z'))
    const props = renderModal()

    fireEvent.change(amountInput(), { target: { value: '100' } })
    fireEvent.change(dateInput(), { target: { value: '2026-10-13' } })
    fireEvent.click(screen.getByText('form.save'))
    expect(screen.getByText('opening_balance.date_in_future')).toBeDefined()

    fireEvent.change(dateInput(), { target: { value: '2026-10-12' } })
    fireEvent.click(screen.getByText('form.save'))
    expect(props.onSave).toHaveBeenCalledWith(100, '2026-10-12')
  })

  it('opens on the current opening balance, and offers to remove it', () => {
    const props = renderModal({ openingBalance: { amount: 1200, asOf: '2026-09-30' } })

    expect(dateInput().value).toBe('2026-09-30')
    expect(amountInput().value).toBe('1200')
    fireEvent.click(screen.getByText('opening_balance.clear'))
    expect(props.onClear).toHaveBeenCalled()
  })

  it('has no corner cross: Cancel closes it, and the focus opens on the form', () => {
    renderModal()

    expect(screen.queryByTestId('dialog-close-button')).toBeNull()
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

    expect(screen.getByText('opening_balance.amount_too_precise')).toBeDefined()
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

  it('reads an amount written without its leading zero', () => {
    const props = renderModal()

    fireEvent.change(amountInput(), { target: { value: '.5' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(props.onSave).toHaveBeenCalledWith(0.5, expect.any(String))
  })

  it('refuses an amount the server would not keep, saying so', () => {
    const props = renderModal()

    fireEvent.change(amountInput(), { target: { value: '1000000000000' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(screen.getByText('opening_balance.amount_too_large')).toBeDefined()
    expect(props.onSave).not.toHaveBeenCalled()
  })
})
