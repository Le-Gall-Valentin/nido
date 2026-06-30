import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { DisableTotpModal } from './DisableTotpModal'
import { TotpCodeError, TotpDisableMaxAttemptsError } from '@/features/totp'
import { RateLimitError, NetworkError, ServerError } from '@/shared/lib'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/shared/ui', () => ({
  Dialog: ({ open, children, onClose }: { open: boolean; children: React.ReactNode; onClose: () => void; title: string; maxWidth?: string }) =>
    open ? <div role="dialog"><button onClick={onClose}>close-dialog</button>{children}</div> : null,
  Button: ({ children, onClick, disabled, type, className }: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean }) => (
    <button onClick={onClick} disabled={disabled} type={type} className={className}>{children}</button>
  ),
}))

vi.mock('@/features/totp', async (importActual) => {
  const actual = await importActual<typeof import('@/features/totp')>()
  return {
    ...actual,
    TotpDigitInput: ({ value, onChange, disabled }: { value: string; onChange: (v: string) => void; disabled?: boolean; autoFocus?: boolean; groupLabel?: string; digitLabel?: (i: number) => string }) => (
      <input aria-label="code" value={value} onChange={e => onChange(e.target.value)} disabled={disabled} />
    ),
  }
})

const DEFAULT_PROPS = {
  open: true,
  onClose: vi.fn(),
  onSuccess: vi.fn(),
  onDisable: vi.fn(),
}

describe('DisableTotpModal', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders nothing when closed', () => {
    const { queryByRole } = render(<DisableTotpModal {...DEFAULT_PROPS} open={false} />)
    expect(queryByRole('dialog')).toBeNull()
  })

  it('renders form when open', () => {
    const { getByLabelText } = render(<DisableTotpModal {...DEFAULT_PROPS} />)
    expect(getByLabelText('code')).toBeDefined()
  })

  it('submit button is disabled when code has fewer than 6 digits', () => {
    const { getByLabelText, getByText } = render(<DisableTotpModal {...DEFAULT_PROPS} />)
    fireEvent.change(getByLabelText('code'), { target: { value: '12345' } })
    expect((getByText('disable.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('calls onDisable with the entered code on submit', async () => {
    const onDisable = vi.fn().mockResolvedValue(undefined)
    const { getByLabelText, getByText } = render(<DisableTotpModal {...DEFAULT_PROPS} onDisable={onDisable} />)
    fireEvent.change(getByLabelText('code'), { target: { value: '123456' } })
    fireEvent.click(getByText('disable.submit'))
    await waitFor(() => expect(onDisable).toHaveBeenCalledWith('123456'))
  })

  it('calls onSuccess after successful disable', async () => {
    const onSuccess = vi.fn()
    const { getByLabelText, getByText } = render(<DisableTotpModal {...DEFAULT_PROPS} onDisable={vi.fn().mockResolvedValue(undefined)} onSuccess={onSuccess} />)
    fireEvent.change(getByLabelText('code'), { target: { value: '123456' } })
    fireEvent.click(getByText('disable.submit'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalled())
  })

  it('shows invalid code error on TotpCodeError', async () => {
    const { getByLabelText, getByText, findByRole } = render(
      <DisableTotpModal {...DEFAULT_PROPS} onDisable={vi.fn().mockRejectedValue(new TotpCodeError())} />
    )
    fireEvent.change(getByLabelText('code'), { target: { value: '123456' } })
    fireEvent.click(getByText('disable.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('disable.error.invalid_code')
  })

  it('shows max attempts error on TotpDisableMaxAttemptsError', async () => {
    const { getByLabelText, getByText, findByRole } = render(
      <DisableTotpModal {...DEFAULT_PROPS} onDisable={vi.fn().mockRejectedValue(new TotpDisableMaxAttemptsError())} />
    )
    fireEvent.change(getByLabelText('code'), { target: { value: '123456' } })
    fireEvent.click(getByText('disable.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('disable.error.max_attempts')
  })

  it('shows rate limit error on RateLimitError', async () => {
    const { getByLabelText, getByText, findByRole } = render(
      <DisableTotpModal {...DEFAULT_PROPS} onDisable={vi.fn().mockRejectedValue(new RateLimitError())} />
    )
    fireEvent.change(getByLabelText('code'), { target: { value: '123456' } })
    fireEvent.click(getByText('disable.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('disable.error.rate_limit')
  })

  it('shows network error on NetworkError', async () => {
    const { getByLabelText, getByText, findByRole } = render(
      <DisableTotpModal {...DEFAULT_PROPS} onDisable={vi.fn().mockRejectedValue(new NetworkError())} />
    )
    fireEvent.change(getByLabelText('code'), { target: { value: '123456' } })
    fireEvent.click(getByText('disable.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('disable.error.network')
  })

  it('shows server error on ServerError', async () => {
    const { getByLabelText, getByText, findByRole } = render(
      <DisableTotpModal {...DEFAULT_PROPS} onDisable={vi.fn().mockRejectedValue(new ServerError())} />
    )
    fireEvent.change(getByLabelText('code'), { target: { value: '123456' } })
    fireEvent.click(getByText('disable.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('disable.error.server')
  })

  it('resets code and error when close is triggered', async () => {
    const onClose = vi.fn()
    const { getByLabelText, getByText, queryByRole } = render(
      <DisableTotpModal {...DEFAULT_PROPS} onClose={onClose} onDisable={vi.fn().mockRejectedValue(new TotpCodeError())} />
    )
    fireEvent.change(getByLabelText('code'), { target: { value: '123456' } })
    fireEvent.click(getByText('disable.submit'))
    await waitFor(() => expect(queryByRole('alert')).not.toBeNull())
    fireEvent.click(getByText('setup.dismiss_profile'))
    expect(onClose).toHaveBeenCalled()
  })
})