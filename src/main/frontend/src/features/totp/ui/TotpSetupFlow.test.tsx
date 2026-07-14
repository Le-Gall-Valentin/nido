import { render, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, afterEach } from 'vitest'
import { TotpSetupFlow } from './TotpSetupFlow'
import type { ITotpEnrollApi } from '../model/ITotpEnrollApi'
import { TotpCodeError, TotpConfirmMaxAttemptsError } from '../model/errors'
import { RateLimitError, NetworkError, ServerError } from '@/shared/lib'

vi.mock('qrcode.react', () => {
  const QRCodeSVG = (props: { value: string }) => <div data-testid="qr-code" data-value={props.value} />
  return { QRCodeSVG }
})

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const SETUP_DATA = {
  otpauthUri: 'otpauth://totp/Nido:test@test.com?secret=ABCDEF',
  secret: 'ABCDEFGHIJKLMNOP',
}

function makeApi(overrides: Partial<ITotpEnrollApi> = {}): ITotpEnrollApi {
  return {
    setup: vi.fn().mockResolvedValue(SETUP_DATA),
    confirm: vi.fn().mockResolvedValue(undefined),
    getStatus: vi.fn(),
    disable: vi.fn(),
    ...overrides,
  }
}

function fillCode(container: HTMLElement, code: string) {
  const inputs = container.querySelectorAll('input[inputmode="numeric"]')
  code.split('').forEach((digit, i) => {
    fireEvent.change(inputs[i]!, { target: { value: digit } })
  })
}

describe('TotpSetupFlow', () => {
  afterEach(() => { vi.useRealTimers() })

  it('shows loading spinner while setup is pending', () => {
    const api = makeApi({ setup: vi.fn().mockReturnValue(new Promise(() => {})) })
    const { getByRole } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)
    expect(getByRole('status')).toBeTruthy()
  })

  it('calls api.setup() exactly once on mount', async () => {
    const setup = vi.fn().mockResolvedValue(SETUP_DATA)
    const api = makeApi({ setup })
    const { findByTestId } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)
    await findByTestId('qr-code')
    expect(setup).toHaveBeenCalledTimes(1)
  })

  it('shows error when setup returns whitespace-only secret', async () => {
    const api = makeApi({ setup: vi.fn().mockResolvedValue({ otpauthUri: 'otpauth://...', secret: '   ' }) })
    const { findByRole } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('setup.error.setup_failed')
  })

  it('shows error when setup returns empty secret', async () => {
    const api = makeApi({ setup: vi.fn().mockResolvedValue({ otpauthUri: 'otpauth://...', secret: '' }) })
    const { findByRole } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('setup.error.setup_failed')
  })

  it('shows error when setup fails', async () => {
    const api = makeApi({ setup: vi.fn().mockRejectedValue(new Error('network error')) })
    const { findByRole } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('setup.error.setup_failed')
  })

  it('renders QR code after setup succeeds', async () => {
    const api = makeApi()
    const { findByTestId } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)

    const qr = await findByTestId('qr-code')
    expect(qr).toBeTruthy()
    expect(qr.getAttribute('data-value')).toBe(SETUP_DATA.otpauthUri)
  })

  it('hides secret by default after setup succeeds', async () => {
    const api = makeApi()
    const { findByTestId, queryByText } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)
    await findByTestId('qr-code')
    // Secret should be masked, not visible
    expect(queryByText('ABCD EFGH IJKL MNOP')).toBeNull()
  })

  it('reveals secret after clicking show button', async () => {
    const api = makeApi()
    const { findByTestId, getByRole, getByText } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)
    await findByTestId('qr-code')

    fireEvent.click(getByRole('button', { name: 'setup.show_secret' }))
    expect(getByText('ABCD EFGH IJKL MNOP')).toBeTruthy()
  })

  it('hides secret again after clicking hide button', async () => {
    const api = makeApi()
    const { findByTestId, getByRole, queryByText } = render(<TotpSetupFlow api={api} onSuccess={vi.fn()} />)
    await findByTestId('qr-code')

    fireEvent.click(getByRole('button', { name: 'setup.show_secret' }))
    fireEvent.click(getByRole('button', { name: 'setup.hide_secret' }))
    expect(queryByText('ABCD EFGH IJKL MNOP')).toBeNull()
  })

  it('calls api.confirm with the entered code on submit', async () => {
    const api = makeApi()
    const { findByTestId, container, getByRole } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })

    expect(api.confirm).toHaveBeenCalledWith('123456')
  })

  it('calls onSuccess after confirm succeeds', async () => {
    const onSuccess = vi.fn()
    const api = makeApi()
    const { findByTestId, container, getByRole } = render(
      <TotpSetupFlow api={api} onSuccess={onSuccess} />
    )
    await findByTestId('qr-code')

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })

    expect(onSuccess).toHaveBeenCalledTimes(1)
  })

  it('shows error and clears code on TotpCodeError from confirm', async () => {
    const api = makeApi({ confirm: vi.fn().mockRejectedValue(new TotpCodeError()) })
    const { findByTestId, container, getByRole, findByRole } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')

    fillCode(container, '999999')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })

    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('setup.error.invalid_code')

    const inputs = container.querySelectorAll('input[inputmode="numeric"]') as NodeListOf<HTMLInputElement>
    inputs.forEach(input => expect(input.value).toBe(''))
  })

  it('renders dismiss button when onDismiss is provided', async () => {
    const api = makeApi()
    const { findByTestId, getByText } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} onDismiss={vi.fn()} />
    )
    await findByTestId('qr-code')
    expect(getByText('setup.dismiss_login')).toBeTruthy()
  })

  it('does not render dismiss button when onDismiss is not provided', async () => {
    const api = makeApi()
    const { findByTestId, queryByText } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')
    expect(queryByText('setup.dismiss_login')).toBeNull()
  })

  it('calls onDismiss when dismiss button clicked', async () => {
    const onDismiss = vi.fn()
    const api = makeApi()
    const { findByTestId, getByText } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} onDismiss={onDismiss} />
    )
    await findByTestId('qr-code')

    fireEvent.click(getByText('setup.dismiss_login'))
    expect(onDismiss).toHaveBeenCalledTimes(1)
  })

  it('uses custom dismissLabel when provided', async () => {
    const api = makeApi()
    const { findByTestId, getByText } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} onDismiss={vi.fn()} dismissLabel="Custom label" />
    )
    await findByTestId('qr-code')
    expect(getByText('Custom label')).toBeTruthy()
  })

  it('shows rate_limit error on RateLimitError from confirm', async () => {
    const api = makeApi({ confirm: vi.fn().mockRejectedValue(new RateLimitError()) })
    const { findByTestId, container, getByRole, findByRole } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')
    const inputs = container.querySelectorAll('input[inputmode="numeric"]')
    '123456'.split('').forEach((d, i) => fireEvent.change(inputs[i]!, { target: { value: d } }))
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('setup.error.rate_limit')
  })

  it('shows network error on NetworkError from confirm', async () => {
    const api = makeApi({ confirm: vi.fn().mockRejectedValue(new NetworkError()) })
    const { findByTestId, container, getByRole, findByRole } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')
    const inputs = container.querySelectorAll('input[inputmode="numeric"]')
    '654321'.split('').forEach((d, i) => fireEvent.change(inputs[i]!, { target: { value: d } }))
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('setup.error.network')
  })

  it('shows max_attempts error on TotpConfirmMaxAttemptsError', async () => {
    const api = makeApi({ confirm: vi.fn().mockRejectedValue(new TotpConfirmMaxAttemptsError()) })
    const { findByTestId, container, getByRole, findByRole } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')
    '123456'.split('').forEach((d, i) =>
      fireEvent.change(container.querySelectorAll('input[inputmode="numeric"]')[i]!, { target: { value: d } })
    )
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('setup.error.max_attempts')
  })

  it('does not restart setup if unmounted within 2s of TotpConfirmMaxAttemptsError', async () => {
    const setup = vi.fn().mockResolvedValue(SETUP_DATA)
    const api = makeApi({ setup, confirm: vi.fn().mockRejectedValue(new TotpConfirmMaxAttemptsError()) })
    const { findByTestId, container, getByRole, unmount } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')
    vi.useFakeTimers()

    '123456'.split('').forEach((d, i) =>
      fireEvent.change(container.querySelectorAll('input[inputmode="numeric"]')[i]!, { target: { value: d } })
    )
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })

    unmount()
    await act(async () => { vi.advanceTimersByTime(2000) })

    // Timer was cancelled on unmount — setup should not be called again
    expect(setup).toHaveBeenCalledTimes(1)
  })

  it('restarts setup automatically 2s after TotpConfirmMaxAttemptsError', async () => {
    const setup = vi.fn().mockResolvedValue(SETUP_DATA)
    const api = makeApi({ setup, confirm: vi.fn().mockRejectedValue(new TotpConfirmMaxAttemptsError()) })
    const { findByTestId, container, getByRole } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')
    expect(setup).toHaveBeenCalledTimes(1)

    // Switch to fake timers BEFORE submit so the setTimeout(2000) in catch is fake
    vi.useFakeTimers()

    '123456'.split('').forEach((d, i) =>
      fireEvent.change(container.querySelectorAll('input[inputmode="numeric"]')[i]!, { target: { value: d } })
    )
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })

    // Advance fake timer — triggers restart
    await act(async () => { vi.advanceTimersByTime(2000) })
    // Flush api.setup() promise from restart
    await act(async () => {})

    expect(setup).toHaveBeenCalledTimes(2)
  })

  it('auto-hides secret after 5 minutes when visible', async () => {
    const api = makeApi()
    const { findByTestId, getByRole, queryByText } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')

    // Switch to fake timers AFTER async setup — click show triggers the timer
    vi.useFakeTimers()
    await act(async () => {
      fireEvent.click(getByRole('button', { name: 'setup.show_secret' }))
    })
    expect(queryByText('ABCD EFGH IJKL MNOP')).toBeTruthy()

    act(() => { vi.advanceTimersByTime(5 * 60 * 1000) })
    expect(queryByText('ABCD EFGH IJKL MNOP')).toBeNull()
    vi.useRealTimers()
  })

  it('shows server error on ServerError from confirm', async () => {
    const api = makeApi({ confirm: vi.fn().mockRejectedValue(new ServerError()) })
    const { findByTestId, container, getByRole, findByRole } = render(
      <TotpSetupFlow api={api} onSuccess={vi.fn()} />
    )
    await findByTestId('qr-code')
    const inputs = container.querySelectorAll('input[inputmode="numeric"]')
    '111111'.split('').forEach((d, i) => fireEvent.change(inputs[i]!, { target: { value: d } }))
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /setup\.submit/i }).closest('form')!)
    })
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('setup.error.server')
  })
})