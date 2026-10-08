import { render, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, afterEach } from 'vitest'
import { CodeStep } from './CodeStep'
import type { ITwoFactorChallengeApi } from '../model/ITwoFactorChallengeApi'
import { CodeError, ChallengeExpiredError, MaxAttemptsError, MethodNotEnabledError, ResendTooSoonError, SendLimitError } from '../model/errors'
import { RateLimitError, NetworkError, ServerError } from '@/shared/lib'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (k: string, opts?: Record<string, unknown>) =>
      opts ? `${k}:${JSON.stringify(opts)}` : k,
  }),
}))

function makeApi(
  overrides: Partial<Pick<ITwoFactorChallengeApi, 'verify' | 'sendMailCode'>> = {},
): Pick<ITwoFactorChallengeApi, 'verify' | 'sendMailCode'> {
  return {
    verify: vi.fn(),
    sendMailCode: vi.fn(),
    ...overrides,
  }
}

function codeField(container: HTMLElement) {
  return container.querySelector('input[autocomplete="one-time-code"]') as HTMLInputElement
}

function fillCode(container: HTMLElement, code: string) {
  fireEvent.change(codeField(container), { target: { value: code } })
}

describe('CodeStep', () => {
  it('renders heading, subtitle with username, submit button, back link, help text', () => {
    const api = makeApi()
    const { getByText, getByRole } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
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
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    expect(api.verify).toHaveBeenCalledWith('APP', '123456')
  })

  it('calls onVerified with user on success', async () => {
    const user = { id: '1', username: 'alice', role: 'USER' as const }
    const api = makeApi({ verify: vi.fn().mockResolvedValue(user) })
    const onVerified = vi.fn()
    const { container, getByRole } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={onVerified} onBack={vi.fn()} />
    )

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    expect(onVerified).toHaveBeenCalledWith(user)
  })

  it('shows invalid_code error and clears code on CodeError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new CodeError()) })
    const { container, getByRole, getByText } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '999999')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    await waitFor(() => {
      expect(getByText('verify.error.invalid_code')).toBeTruthy()
    })

    // Code should be cleared
    expect(codeField(container).value).toBe('')
  })

  it('shows challenge_expired error on ChallengeExpiredError (does not clear code)', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new ChallengeExpiredError()) })
    const { container, getByRole, getByText } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )

    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })

    await waitFor(() => {
      expect(getByText('verify.error.challenge_expired')).toBeTruthy()
    })

    // Code should NOT be cleared
    expect(codeField(container).value).toBe('123456')
  })

  it('shows incomplete error without calling api if code has fewer than 6 digits', async () => {
    const api = makeApi({ verify: vi.fn() })
    const { container, getByRole, getByText } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
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
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
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
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
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
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
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
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={onBack} />
    )

    fireEvent.click(getByText('verify.back'))
    expect(onBack).toHaveBeenCalledTimes(1)
  })

  it('shows rate_limit_timed error with seconds when RateLimitError has retryAfterSeconds', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new RateLimitError(42)) })
    const { container, getByRole, getByText } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('twoFactor:error.rate_limit_timed:{"seconds":42}')).toBeTruthy())
  })

  it('shows rate_limit error on RateLimitError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new RateLimitError()) })
    const { container, getByRole, getByText } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('twoFactor:error.rate_limit')).toBeTruthy())
  })

  it('shows network error on NetworkError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new NetworkError()) })
    const { container, getByRole, getByText } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('twoFactor:error.network')).toBeTruthy())
  })

  it('shows server error on ServerError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new ServerError()) })
    const { container, getByRole, getByText } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
    )
    fillCode(container, '123456')
    await act(async () => {
      fireEvent.submit(getByRole('button', { name: /verify\.submit/i }).closest('form')!)
    })
    await waitFor(() => expect(getByText('twoFactor:error.server')).toBeTruthy())
  })

  it('shows max_attempts error on MaxAttemptsError', async () => {
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new MaxAttemptsError()) })
    const { container, getByRole, getByText } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={vi.fn()} />
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
    const api = makeApi({ verify: vi.fn().mockRejectedValue(new MaxAttemptsError()) })
    const onBack = vi.fn()
    const { container, getByRole } = render(
      <CodeStep username="alice" method="APP" api={api} onVerified={vi.fn()} onBack={onBack} />
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

/**
 * React starts the wait from an effect it flushes on its own scheduler: on a busy machine, only once the event loop
 * has turned — a synchronous act() never lets it, and the wait would never start. Let it turn, then let the wait pass.
 */
async function expectBackAfterTheWait(onBack: ReturnType<typeof vi.fn>) {
  await act(async () => { await new Promise(resolve => setImmediate(resolve)) })
  act(() => { vi.advanceTimersByTime(2000) })
  expect(onBack).toHaveBeenCalled()
}

describe('CodeStep by mail', () => {
  const mailApi = (overrides: Partial<Pick<ITwoFactorChallengeApi, 'verify' | 'sendMailCode'>> = {}) => ({
    verify: vi.fn(), sendMailCode: vi.fn(), ...overrides,
  })

  it('says where the code went and waits before offering another', () => {
    vi.useFakeTimers()
    const { getByText, queryByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" resendAfterSeconds={2}
        api={mailApi()} onVerified={vi.fn()} onBack={vi.fn()} />)

    expect(getByText('mail.title')).toBeTruthy()
    expect(getByText('mail.subtitle:{"username":"camille","address":"c••••••n@exemple.fr"}')).toBeTruthy()
    expect(getByText('resend.wait:{"time":"0:02"}')).toBeTruthy()
    act(() => { vi.advanceTimersByTime(2000) })
    expect(queryByText('resend.action')).toBeTruthy()
    vi.useRealTimers()
  })

  it('verifies with the mail method', async () => {
    const verify = vi.fn().mockResolvedValue({ id: '1', username: 'camille' })
    const onVerified = vi.fn()
    const { container, getByRole } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" api={mailApi({ verify })}
        onVerified={onVerified} onBack={vi.fn()} />)

    fillCode(container, '004213')
    fireEvent.click(getByRole('button', { name: /verify\.submit/ }))

    await waitFor(() => expect(onVerified).toHaveBeenCalled())
    expect(verify).toHaveBeenCalledWith('MAIL', '004213')
  })

  it('a new code restarts the wait and says the old one is gone', async () => {
    const sendMailCode = vi.fn().mockResolvedValue({ resendAfterSeconds: 60 })
    const { getByText, findByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" resendAfterSeconds={0}
        api={mailApi({ sendMailCode })} onVerified={vi.fn()} onBack={vi.fn()} />)

    fireEvent.click(getByText('resend.action'))

    expect(await findByText('mail.resent')).toBeTruthy()
    expect(getByText('resend.wait:{"time":"1:00"}')).toBeTruthy()
  })

  it('a code the login could not send says when one can leave', () => {
    const { getByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" mailLimitSeconds={420}
        api={mailApi()} onVerified={vi.fn()} onBack={vi.fn()} />)

    expect(getByText('twoFactor:error.send_limit:{"minutes":7}')).toBeTruthy()
    expect(getByText('resend.wait:{"time":"7:00"}')).toBeTruthy()
    // Nothing left: the subtitle must not say a code was sent.
    expect(getByText('mail.subtitle_not_sent:{"username":"camille","address":"c••••••n@exemple.fr"}')).toBeTruthy()
  })

  it('once a code finally leaves, the subtitle says it went', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const sendMailCode = vi.fn().mockResolvedValue({ resendAfterSeconds: 60 })
    const { getByText, findByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" mailLimitSeconds={1}
        api={mailApi({ sendMailCode })} onVerified={vi.fn()} onBack={vi.fn()} />)
    act(() => { vi.advanceTimersByTime(1000) })

    fireEvent.click(getByText('resend.action'))

    expect(await findByText('mail.subtitle:{"username":"camille","address":"c••••••n@exemple.fr"}')).toBeTruthy()
    vi.useRealTimers()
  })

  it('a method removed meanwhile is said, then the sign-in starts again', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const onBack = vi.fn()
    const { container, getByRole, findByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr"
        api={mailApi({ verify: vi.fn().mockRejectedValue(new MethodNotEnabledError()) })} onVerified={vi.fn()} onBack={onBack} />)

    fillCode(container, '004213')
    fireEvent.click(getByRole('button', { name: /verify\.submit/ }))

    expect(await findByText('verify.error.method_not_enabled')).toBeTruthy()
    await expectBackAfterTheWait(onBack)
    vi.useRealTimers()
  })

  it('with two methods, the way back is the choice', () => {
    const onChooseAnother = vi.fn()
    const { getByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" api={mailApi()}
        onVerified={vi.fn()} onBack={vi.fn()} onChooseAnother={onChooseAnother} />)

    fireEvent.click(getByText('verify.back_choose'))

    expect(onChooseAnother).toHaveBeenCalled()
  })

  it('asking again within the minute says a code just left, and when another can', async () => {
    // Another tab of the same browser signed in moments ago: its code is the live one, in the mailbox.
    const sendMailCode = vi.fn().mockRejectedValue(new ResendTooSoonError(20))
    const { getByText, findByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" resendAfterSeconds={0}
        api={mailApi({ sendMailCode })} onVerified={vi.fn()} onBack={vi.fn()} />)

    fireEvent.click(getByText('resend.action'))

    expect(await findByText('mail.recent')).toBeTruthy()
    expect(getByText('resend.wait:{"time":"0:20"}')).toBeTruthy()
  })

  it('a resend stopped by the route\'s limit says how long to wait', async () => {
    const sendMailCode = vi.fn().mockRejectedValue(new RateLimitError(30))
    const { getByText, findByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" resendAfterSeconds={0}
        api={mailApi({ sendMailCode })} onVerified={vi.fn()} onBack={vi.fn()} />)

    fireEvent.click(getByText('resend.action'))

    expect(await findByText('twoFactor:error.rate_limit_timed:{"seconds":30}')).toBeTruthy()
  })

  it('a resend stopped by the account limit says when, and waits that long', async () => {
    const sendMailCode = vi.fn().mockRejectedValue(new SendLimitError(600))
    const { getByText, findByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" resendAfterSeconds={0}
        api={mailApi({ sendMailCode })} onVerified={vi.fn()} onBack={vi.fn()} />)

    fireEvent.click(getByText('resend.action'))

    expect(await findByText('twoFactor:error.send_limit:{"minutes":10}')).toBeTruthy()
    expect(getByText('resend.wait:{"time":"10:00"}')).toBeTruthy()
  })

  it('a resend met by the lockout is said, then the sign-in starts again', async () => {
    // Another tab used up the codes: no code leaves into a sign-in that would refuse it.
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const onBack = vi.fn()
    const { getByText, findByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" resendAfterSeconds={0}
        api={mailApi({ sendMailCode: vi.fn().mockRejectedValue(new MaxAttemptsError()) })} onVerified={vi.fn()} onBack={onBack} />)

    fireEvent.click(getByText('resend.action'))

    expect(await findByText('verify.error.max_attempts')).toBeTruthy()
    await expectBackAfterTheWait(onBack)
    vi.useRealTimers()
  })

  it('a second click on resend while the first is on its way sends nothing', () => {
    const sendMailCode = vi.fn(() => new Promise<{ resendAfterSeconds: number }>(() => {}))
    const { getByText } = render(
      <CodeStep username="camille" method="MAIL" maskedEmail="c••••••n@exemple.fr" resendAfterSeconds={0}
        api={mailApi({ sendMailCode })} onVerified={vi.fn()} onBack={vi.fn()} />)

    const resend = getByText('resend.action')
    fireEvent.click(resend)
    fireEvent.click(resend)

    expect(sendMailCode).toHaveBeenCalledTimes(1)
    expect((resend as HTMLButtonElement).disabled).toBe(true)
  })
})
