import { render, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAuth } from '@/features/auth'
import { CredentialsError, NetworkError, RateLimitError, ServerError } from '../model/errors'
import { LoginForm } from './LoginForm'

vi.mock('@/features/auth/model/authStoreContext', () => ({ useAuth: vi.fn() }))
vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

const mockUseAuth = vi.mocked(useAuth)

beforeEach(() => vi.clearAllMocks())

function setup(mockLogin: ReturnType<typeof vi.fn>, props: { onLoginOutcome?: (outcome: unknown) => void } = {}) {
  const baseState = {
    user: null,
    isInitializing: false,
    login: mockLogin,
    logout: vi.fn(),
    initialize: vi.fn(),
    finalizeLogin: vi.fn(),
    patchUser: vi.fn(),
  }
  mockUseAuth.mockImplementation((selector) => selector(baseState as Parameters<typeof selector>[0]))
  return render(<LoginForm {...props} />)
}

describe('LoginForm', () => {
  it('calls login with credentials on submit', async () => {
    const mockLogin = vi.fn().mockResolvedValue({ kind: 'authenticated' })
    const { getByLabelText, queryByRole } = setup(mockLogin)

    fireEvent.change(getByLabelText('field.username'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'secret' } })
    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() =>
      expect(mockLogin).toHaveBeenCalledWith({ username: 'alice', password: 'secret' })
    )
    expect(queryByRole('alert')).toBeNull()
  })

  it('shows credentials error and clears password on CredentialsError', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new CredentialsError())
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.change(getByLabelText('field.username'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'wrong' } })
    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.credentials')
    })
    expect((getByLabelText('field.password') as HTMLInputElement).value).toBe('')
  })

  it('shows network error on NetworkError', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new NetworkError())
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.network')
    })
  })

  it('shows rateLimit error on RateLimitError', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new RateLimitError())
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.rateLimit')
    })
  })

  it('shows rateLimit error with delay when retryAfterSeconds is set', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new RateLimitError(42))
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.rateLimitWithDelay')
    })
  })

  it('shows server error on ServerError', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new ServerError())
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.server')
    })
  })

  it('toggles password visibility', async () => {
    const { container, getByLabelText } = setup(vi.fn())
    const passwordInput = getByLabelText('field.password') as HTMLInputElement
    expect(passwordInput.type).toBe('password')

    const showBtn = container.querySelector('[aria-label="field.show_password"]') as HTMLButtonElement
    await act(async () => { fireEvent.click(showBtn) })
    expect(passwordInput.type).toBe('text')

    const hideBtn = container.querySelector('[aria-label="field.hide_password"]') as HTMLButtonElement
    await act(async () => { fireEvent.click(hideBtn) })
    expect(passwordInput.type).toBe('password')
  })

  it('disables submit button while loading', async () => {
    let resolveLogin!: (value: { kind: 'authenticated' }) => void
    const pendingPromise = new Promise<{ kind: 'authenticated' }>((resolve) => { resolveLogin = resolve })
    const mockLogin = vi.fn().mockReturnValue(pendingPromise)
    const { getByLabelText, container } = setup(mockLogin)

    await act(async () => {
      fireEvent.submit(getByLabelText('field.username').closest('form')!)
      // flush microtasks so React commits setIsLoading(true)
      await Promise.resolve()
    })

    const submitBtn = container.querySelector('button[type="submit"]') as HTMLButtonElement
    expect(submitBtn.disabled).toBe(true)

    await act(async () => { resolveLogin({ kind: 'authenticated' }) })
    expect(submitBtn.disabled).toBe(false)
  })

  it('calls onLoginOutcome when login returns totp_required', async () => {
    const mockLogin = vi.fn().mockResolvedValue({ kind: 'totp_required', username: 'alice' })
    const onLoginOutcome = vi.fn()
    const { getByLabelText } = setup(mockLogin, { onLoginOutcome })

    fireEvent.change(getByLabelText('field.username'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'secret' } })
    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() => expect(onLoginOutcome).toHaveBeenCalledWith({ kind: 'totp_required', username: 'alice' }))
  })

  it('calls onLoginOutcome when login returns enrollment_proposed', async () => {
    const user = { id: '1', username: 'alice', role: 'USER' }
    const mockLogin = vi.fn().mockResolvedValue({ kind: 'enrollment_proposed', user })
    const onLoginOutcome = vi.fn()
    const { getByLabelText } = setup(mockLogin, { onLoginOutcome })

    fireEvent.change(getByLabelText('field.username'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'secret' } })
    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() => expect(onLoginOutcome).toHaveBeenCalledWith({ kind: 'enrollment_proposed', user }))
  })

  it('does not call onLoginOutcome when login returns authenticated', async () => {
    const mockLogin = vi.fn().mockResolvedValue({ kind: 'authenticated' })
    const onLoginOutcome = vi.fn()
    const { getByLabelText } = setup(mockLogin, { onLoginOutcome })

    fireEvent.change(getByLabelText('field.username'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'secret' } })
    fireEvent.submit(getByLabelText('field.username').closest('form')!)

    await waitFor(() => expect(mockLogin).toHaveBeenCalled())
    expect(onLoginOutcome).not.toHaveBeenCalled()
  })

  it('prevents double-submit — login called only once for concurrent submits', async () => {
    let resolveLogin!: (value: { kind: 'authenticated' }) => void
    const pendingPromise = new Promise<{ kind: 'authenticated' }>((resolve) => { resolveLogin = resolve })
    const mockLogin = vi.fn().mockReturnValue(pendingPromise)
    const { getByLabelText } = setup(mockLogin)
    const form = getByLabelText('field.username').closest('form')!

    await act(async () => {
      fireEvent.submit(form)
      fireEvent.submit(form)
      await Promise.resolve()
    })

    expect(mockLogin).toHaveBeenCalledTimes(1)
    await act(async () => { resolveLogin({ kind: 'authenticated' }) })
  })
})