import { render, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAuth } from '@/features/auth'
import { CredentialsError, NetworkError, RateLimitError, ServerError } from '../model/errors'
import { LoginForm } from './LoginForm'

vi.mock('@/features/auth/model/authStoreContext', () => ({ useAuth: vi.fn() }))
vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

const mockUseAuth = vi.mocked(useAuth)

beforeEach(() => vi.clearAllMocks())

function setup(mockLogin: ReturnType<typeof vi.fn>, props: { onLoginOutcome?: (outcome: unknown) => void; initialIdentifier?: string } = {}) {
  const baseState = {
    user: null,
    isInitializing: false,
    signedOut: false,
    signingIn: null,
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
  it('starts from the identifier it is given, and puts the cursor in the password', () => {
    const { getByLabelText } = setup(vi.fn(), { initialIdentifier: 'carol' })

    expect((getByLabelText('field.identifier') as HTMLInputElement).value).toBe('carol')
    expect(document.activeElement).toBe(getByLabelText('field.password'))
  })

  it('calls login with credentials on submit', async () => {
    const mockLogin = vi.fn().mockResolvedValue({ kind: 'authenticated' })
    const { getByLabelText, queryByRole } = setup(mockLogin)

    fireEvent.change(getByLabelText('field.identifier'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'secret' } })
    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() =>
      expect(mockLogin).toHaveBeenCalledWith({ identifier: 'alice', password: 'secret' })
    )
    expect(queryByRole('alert')).toBeNull()
  })

  it('shows credentials error and clears password on CredentialsError', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new CredentialsError())
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.change(getByLabelText('field.identifier'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'wrong' } })
    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.credentials')
    })
    expect((getByLabelText('field.password') as HTMLInputElement).value).toBe('')
  })

  it('shows network error on NetworkError', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new NetworkError())
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.network')
    })
  })

  it('shows rateLimit error on RateLimitError', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new RateLimitError())
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.rateLimit')
    })
  })

  it('shows rateLimit error with delay when retryAfterSeconds is set', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new RateLimitError(42))
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.rateLimitWithDelay')
    })
  })

  it('shows server error on ServerError', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new ServerError())
    const { getByLabelText, getByRole } = setup(mockLogin)

    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() => {
      const alert = getByRole('alert')
      expect(alert.textContent).toContain('error.server')
    })
  })

  it('toggles password visibility', async () => {
    const { container, getByLabelText } = setup(vi.fn())
    const passwordInput = getByLabelText('field.password') as HTMLInputElement
    expect(passwordInput.type).toBe('password')

    const showBtn = container.querySelector('[aria-label="password.show"]') as HTMLButtonElement
    await act(async () => { fireEvent.click(showBtn) })
    expect(passwordInput.type).toBe('text')

    const hideBtn = container.querySelector('[aria-label="password.hide"]') as HTMLButtonElement
    await act(async () => { fireEvent.click(hideBtn) })
    expect(passwordInput.type).toBe('password')
  })

  it('disables submit button while loading', async () => {
    let resolveLogin!: (value: { kind: 'authenticated' }) => void
    const pendingPromise = new Promise<{ kind: 'authenticated' }>((resolve) => { resolveLogin = resolve })
    const mockLogin = vi.fn().mockReturnValue(pendingPromise)
    const { getByLabelText, container } = setup(mockLogin)

    await act(async () => {
      fireEvent.submit(getByLabelText('field.identifier').closest('form')!)
      // flush microtasks so React commits setIsLoading(true)
      await Promise.resolve()
    })

    const submitBtn = container.querySelector('button[type="submit"]') as HTMLButtonElement
    expect(submitBtn.disabled).toBe(true)

    await act(async () => { resolveLogin({ kind: 'authenticated' }) })
    expect(submitBtn.disabled).toBe(false)
  })

  it('calls onLoginOutcome when login asks for a second factor', async () => {
    const outcome = { kind: 'two_factor_required', challenge: { username: 'alice', methods: ['APP'], maskedEmail: null, mailCode: null } }
    const mockLogin = vi.fn().mockResolvedValue(outcome)
    const onLoginOutcome = vi.fn()
    const { getByLabelText } = setup(mockLogin, { onLoginOutcome })

    fireEvent.change(getByLabelText('field.identifier'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'secret' } })
    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() => expect(onLoginOutcome).toHaveBeenCalledWith(outcome))
  })

  it('calls onLoginOutcome when login returns enrollment_proposed', async () => {
    const user = { id: '1', username: 'alice', role: 'USER' }
    const mockLogin = vi.fn().mockResolvedValue({ kind: 'enrollment_proposed', user })
    const onLoginOutcome = vi.fn()
    const { getByLabelText } = setup(mockLogin, { onLoginOutcome })

    fireEvent.change(getByLabelText('field.identifier'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'secret' } })
    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() => expect(onLoginOutcome).toHaveBeenCalledWith({ kind: 'enrollment_proposed', user }))
  })

  it('does not call onLoginOutcome when login returns authenticated', async () => {
    const mockLogin = vi.fn().mockResolvedValue({ kind: 'authenticated' })
    const onLoginOutcome = vi.fn()
    const { getByLabelText } = setup(mockLogin, { onLoginOutcome })

    fireEvent.change(getByLabelText('field.identifier'), { target: { value: 'alice' } })
    fireEvent.change(getByLabelText('field.password'), { target: { value: 'secret' } })
    fireEvent.submit(getByLabelText('field.identifier').closest('form')!)

    await waitFor(() => expect(mockLogin).toHaveBeenCalled())
    expect(onLoginOutcome).not.toHaveBeenCalled()
  })

  it('takes up to 254 characters and keeps the username autocomplete token', () => {
    const { getByLabelText } = setup(vi.fn())
    const field = getByLabelText('field.identifier') as HTMLInputElement
    expect(field.maxLength).toBe(254)
    expect(field.getAttribute('autocomplete')).toBe('username')
    expect(field.name).toBe('identifier')
  })

  it('takes the identifier as typed — no capital or correction from a phone keyboard', () => {
    // Chrome and Safari capitalise the first letter by default: "jane" would arrive as "Jane".
    const { getByLabelText } = setup(vi.fn())
    const field = getByLabelText('field.identifier') as HTMLInputElement
    expect(field.getAttribute('autocapitalize')).toBe('off')
    expect(field.getAttribute('autocorrect')).toBe('off')
    expect(field.getAttribute('spellcheck')).toBe('false')
  })

  it('prevents double-submit — login called only once for concurrent submits', async () => {
    let resolveLogin!: (value: { kind: 'authenticated' }) => void
    const pendingPromise = new Promise<{ kind: 'authenticated' }>((resolve) => { resolveLogin = resolve })
    const mockLogin = vi.fn().mockReturnValue(pendingPromise)
    const { getByLabelText } = setup(mockLogin)
    const form = getByLabelText('field.identifier').closest('form')!

    await act(async () => {
      fireEvent.submit(form)
      fireEvent.submit(form)
      await Promise.resolve()
    })

    expect(mockLogin).toHaveBeenCalledTimes(1)
    await act(async () => { resolveLogin({ kind: 'authenticated' }) })
  })
})