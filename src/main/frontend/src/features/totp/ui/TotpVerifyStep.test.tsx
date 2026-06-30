import { render, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, afterEach } from 'vitest'
import { TotpVerifyStep } from './TotpVerifyStep'
import type { ITotpVerifyApi } from '../model/ITotpVerifyApi'
import { TotpCodeError, TotpChallengeExpiredError, TotpMaxAttemptsError } from '../model/errors'
import { RateLimitError, NetworkError, ServerError } from '@/shared/lib'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (k: string, opts?: Record<string, unknown>) =>
      opts ? `${k}:${JSON.stringify(opts)}` : k,
  }),
}))

function makeApi(overrides: Partial<ITotpVerifyApi> = {}): ITotpVerifyApi {
  return {
    verify: vi.fn(),
    ...overrides,
  }
}

function fillCode(container: HTMLElement, code: string) {
  const inputs = container.querySelectorAll('input[inputmode="numeric"]')
  code.split('').forEach((digit, i) => {
    fireEvent.change(inputs[i]!, { target: { value: digit } })
  })
}

describe('TotpVerifyStep', () => {
  it('renders heading, subtitle with username, submit button, back link, help text', () => {
    const api = makeApi()
    const { getByText, getByRole } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    expect(getByText('verify.title')).toBeTruthy()
    expect(getByText(`verify.subtitle:${JSON.stringify({ username: 'alice' })}`)).toBeTruthy()
    expect(getByRole('button', { name: /verify\.submit/i })).toBeTruthy()
    expect(getByText('verify.back')).toBeTruthy()
    expect(getByText('verify.help')).toBeTruthy()
  })

  it('calls api.verify with the entered code on submit', async () => {
    const api = makeApi({ verify: vi.fn().mockResolvedValue({ id: '1', username: 'alice', role: 'USER' }) })
    const { container, getByRole } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    expect(api.verify).toHaveBeenCalledWith('123456')
  })

  it('calls onVerified with user on success', async () => {
    const user = { id: '1', username: 'alice', role: 'USER' as const }
    const api = makeApi({ verify: vi.fn().mockResolvedValue(user) })
    const onVerified = vi.fn()
    const { container, getByRole } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={onVerified} onBack={vi.fn()} />
    )

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    expect(onVerified).toHaveBeenCalledWith(user)
  })

  it('shows invalid_code error and clears code on TotpCodeError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new TotpCodeError()) })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '999999')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    await waitFor(() => {
      expect(getByText('verify.error.invalid_code')).toBeTruthy()
    })

    // Code should be cleared
    const inputs = container.querySelectorAll('input[inputmode="numeric"]') as NodeListOf<HTMLInputElement>
    inputs.forEach(input => expect(input.value).toBe(''))
  })

  it('shows challenge_expired error on TotpChallengeExpiredError (does not clear code)', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new TotpChallengeExpiredError()) })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    await waitFor(() => {
      expect(getByText('verify.error.challenge_expired')).toBeTruthy()
    })

    // Code should NOT be cleared
    const inputs = container.querySelectorAll('input[inputmode="numeric"]') as NodeListOf<HTMLInputElement>
    const filledDigits = Array.from(inputs).map(i => i.value).join('')
    expect(filledDigits).toBe('123456')
  })

  it('shows incomplete error without calling api if code has fewer than 6 digits', async () => {
    const api = makeApi({ verify: vi.fn() })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '123')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    expect(getByText('verify.error.incomplete')).toBeTruthy()
    expect(api.verify).not.toHaveBeenCalled()
  })

  it('prevents double-submit', async () => {
    let resolveVerify!: (v: unknown) => void
    const verifyPromise = new Promise(res => { resolveVerify = res })
    const api = makeApi({ verify: vi.fn().mockReturnValue(verifyPromise) })
    const { container, getByRole } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '123456')
    const form = getByRole('button', { name: /verify\.submit/i }).closest('form')!

    await act(async () => {
      fireEvent.submit(form)
    })
    await act(async () => {
      fireEvent.submit(form)
    })

    resolveVerify({ id: '1', username: 'alice', role: 'USER' })
    await act(async () => { await verifyPromise })

    expect(api.verify).toHaveBeenCalledTimes(1)
  })

  it('submit button shows loading state while api call is in flight', async () => {
    let resolveVerify!: (v: unknown) => void
    const verifyPromise = new Promise(res => { resolveVerify = res })
    const api = makeApi({ verify: vi.fn().mockReturnValue(verifyPromise) })
    const { container, getByRole } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '123456')

    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    const button = getByRole('button', { name: /verify\.submit/i })
    expect(button.getAttribute('aria-busy')).toBe('true')
    expect((button as HTMLButtonElement).disabled).toBe(true)

    resolveVerify({ id: '1', username: 'alice', role: 'USER' })
    await act(async () => { await verifyPromise })
  })

  it('back button is disabled while isLoading', async () => {
    let resolveVerify!: (v: unknown) => void
    const verifyPromise = new Promise(res => { resolveVerify = res })
    const api = makeApi({ verify: vi.fn().mockReturnValue(verifyPromise) })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    const backButton = getByText('verify.back').closest('button')!
    expect((backButton as HTMLButtonElement).disabled).toBe(true)

    resolveVerify({ id: '1', username: 'alice', role: 'USER' })
    await act(async () => { await verifyPromise })
  })

  it('calls onBack when back link is clicked', () => {
    const api = makeApi()
    const onBack = vi.fn()
    const { getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={onBack} />
    )

    fireEvent.click(getByText('verify.back'))
    expect(onBack).toHaveBeenCalledTimes(1)
  })

  it('shows rate_limit_timed error with seconds when RateLimitError has retryAfterSeconds', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new RateLimitError(42)) })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('verify.error.rate_limit_timed:{"seconds":42}')).toBeTruthy())
  })

  it('shows rate_limit error on RateLimitError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new RateLimitError()) })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('verify.error.rate_limit')).toBeTruthy())
  })

  it('shows network error on NetworkError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new NetworkError()) })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('verify.error.network')).toBeTruthy())
  })

  it('shows server error on ServerError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new ServerError()) })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('verify.error.server')).toBeTruthy())
  })

  it('shows max_attempts error on TotpMaxAttemptsError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new TotpMaxAttemptsError()) })
    const { container, getByRole, getByText } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('verify.error.max_attempts')).toBeTruthy())
  })

  afterEach(() => { vi.useRealTimers() })

  it('calls onBack automatically after max_attempts lockout', async () => {
    vi.useFakeTimers()
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new TotpMaxAttemptsError()) })
    const onBack = vi.fn()
    const { container, getByRole } = render(
      <TotpVerifyStep username="alice" api={api} onVerified={vi.fn()} onBack={onBack} />
    )
    fillCode(container, '123456')

    // Submit and flush all async work before activating fake timers
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    // At this point error is shown and autoBack=true; now advance fake timers
    expect(onBack).not.toHaveBeenCalled()
    act(() => { vi.runAllTimers() })
    expect(onBack).toHaveBeenCalledTimes(1)
  })
})