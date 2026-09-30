import { StrictMode } from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { StoreApi } from 'zustand'
import * as auth from '@/features/auth'
import type { User } from '@/entities/user'
import { InvalidResetLinkError, type IPasswordResetApi } from '@/features/password-reset'
import { NetworkError } from '@/shared/lib'
import { createTestQueryClient } from '@/shared/test'
import { ResetPasswordPage } from './ResetPasswordPage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: object) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

interface FakeAuth {
  user: User | null
  isInitializing: boolean
  logout: () => Promise<void>
}

// A real store behind the mocked hook: signing out re-renders the page the way the app does.
vi.mock('@/features/auth', async () => {
  const { create, useStore } = await import('zustand')
  const store = create<FakeAuth>((set) => ({
    user: null,
    isInitializing: false,
    logout: vi.fn(async () => { set({ user: null }) }),
  }))
  return {
    useAuth: <T,>(selector: (state: FakeAuth) => T) => useStore(store, selector),
    __store: store,
  }
})

const authStore = (auth as unknown as { __store: StoreApi<FakeAuth> }).__store
const jane: User = {
  id: 'u-1', username: 'jane', email: 'jane@test.com', role: 'USER', createdAt: '2026-01-01T00:00:00Z', totpEnabled: false,
}

beforeEach(() => {
  authStore.setState({ user: null, isInitializing: false })
})

function Where() {
  const location = useLocation()
  return <output data-testid="where">{JSON.stringify({ path: location.pathname, hash: location.hash, state: location.state })}</output>
}

/** Opens another reset link in the same tab — a navigation within the page, not a reload. */
function OpenAnotherLink() {
  const navigate = useNavigate()
  return <button type="button" onClick={() => void navigate('/reset-password#token=def')}>another link</button>
}

function open(url: string, api: Partial<IPasswordResetApi>, { strict = false } = {}) {
  const full: IPasswordResetApi = {
    capabilities: vi.fn(), requestReset: vi.fn(), checkToken: vi.fn().mockResolvedValue(undefined), confirmReset: vi.fn(), ...api,
  }
  const tree = (
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter initialEntries={[url]}>
        <Routes>
          <Route path="/reset-password" element={<><ResetPasswordPage api={full} /><Where /><OpenAnotherLink /></>} />
          <Route path="/login" element={<Where />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
  render(strict ? <StrictMode>{tree}</StrictMode> : tree)
  return full
}

const where = () => JSON.parse(screen.getByTestId('where').textContent ?? '{}') as { path: string; hash: string; state: unknown }

describe('ResetPasswordPage', () => {
  it('checks the link at once and takes the token out of the address bar', async () => {
    const api = open('/reset-password#token=abc', {})

    await waitFor(() => expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.title'))
    expect(api.checkToken).toHaveBeenCalledWith('abc')
    expect(where().hash).toBe('')
  })

  it('checks the link once, even when React mounts the page twice to find side effects', async () => {
    const api = open('/reset-password#token=abc', {}, { strict: true })

    await screen.findByLabelText('field.new_password')
    expect(api.checkToken).toHaveBeenCalledTimes(1)
  })

  it('reads the token among other hash parameters', async () => {
    const api = open('/reset-password#foo=1&token=abc', {})

    await waitFor(() => expect(api.checkToken).toHaveBeenCalledWith('abc'))
  })

  it('without a token it says the link is invalid without asking the server', () => {
    const api = open('/reset-password', {})

    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.invalid.title')
    expect(api.checkToken).not.toHaveBeenCalled()
    expect(screen.getByRole('link', { name: 'reset.invalid.again' }).getAttribute('href')).toBe('/forgot-password')
  })

  it('says so when the link no longer works', async () => {
    open('/reset-password#token=abc', { checkToken: vi.fn().mockRejectedValue(new InvalidResetLinkError()) })

    await waitFor(() => expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.invalid.title'))
  })

  it('offers to try again when the link could not be checked', async () => {
    const checkToken = vi.fn().mockRejectedValueOnce(new NetworkError()).mockResolvedValueOnce(undefined)
    open('/reset-password#token=abc', { checkToken })

    fireEvent.click(await screen.findByRole('button', { name: 'reset.retry' }))

    await waitFor(() => expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.title'))
    expect(checkToken).toHaveBeenCalledTimes(2)
  })

  it('names the page while the link is being checked', () => {
    open('/reset-password#token=abc', { checkToken: vi.fn(() => new Promise<void>(() => {})) })
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.title')
  })

  it('names the page when the link could not be checked', async () => {
    open('/reset-password#token=abc', { checkToken: vi.fn().mockRejectedValue(new NetworkError()) })

    await screen.findByRole('button', { name: 'reset.retry' })
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.title')
  })

  it('says aloud that the link stopped working while the new password was typed', async () => {
    const confirmReset = vi.fn().mockRejectedValue(new InvalidResetLinkError())
    open('/reset-password#token=abc', { confirmReset })
    await screen.findByLabelText('field.new_password')

    fireEvent.change(screen.getByLabelText('field.new_password'), { target: { value: 'NewPassw0rd!' } })
    fireEvent.change(screen.getByLabelText('field.confirm'), { target: { value: 'NewPassw0rd!' } })
    fireEvent.click(screen.getByRole('button', { name: 'action.save' }))

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('reset.invalid.title')
    expect(screen.queryByLabelText('field.new_password')).toBeNull()
  })

  it('follows a newer link opened in the same tab instead of keeping the old token', async () => {
    const checkToken = vi.fn((token: string) => (token === 'abc' ? Promise.reject(new InvalidResetLinkError()) : Promise.resolve()))
    open('/reset-password#token=abc', { checkToken })
    await waitFor(() => expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.invalid.title'))

    fireEvent.click(screen.getByRole('button', { name: 'another link' }))

    await screen.findByLabelText('field.new_password')
    expect(checkToken).toHaveBeenLastCalledWith('def')
    expect(where().hash).toBe('')
  })

  it('asks a signed-in person to sign out first, the link kept out of the address bar', async () => {
    authStore.setState({ user: jane })
    open('/reset-password#token=abc', {})

    await waitFor(() => expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.signed_in.title'))
    expect(screen.getByText('reset.signed_in.body:{"username":"jane"}')).not.toBeNull()
    expect(screen.getByRole('link', { name: 'reset.signed_in.back' }).getAttribute('href')).toBe('/')
    expect(screen.queryByLabelText('field.new_password')).toBeNull()
    expect(where().hash).toBe('')
  })

  it('signs out and goes on with the same link', async () => {
    authStore.setState({ user: jane })
    const api = open('/reset-password#token=abc', {})

    fireEvent.click(await screen.findByRole('button', { name: 'reset.signed_in.sign_out' }))

    await screen.findByLabelText('field.new_password')
    expect(authStore.getState().logout).toHaveBeenCalled()
    expect(api.checkToken).toHaveBeenCalledWith('abc')
  })

  it('waits while a session is being restored, rather than show a form to someone signed in', async () => {
    authStore.setState({ isInitializing: true })
    const api = open('/reset-password#token=abc', {})

    // The link itself checks out: only the session still being restored holds the form back.
    await waitFor(() => expect(api.checkToken).toHaveBeenCalled())
    await act(async () => {})
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('reset.title')
    expect(screen.queryByLabelText('field.new_password')).toBeNull()
  })

  it('sends the person to the login page, told the password changed', async () => {
    const confirmReset = vi.fn().mockResolvedValue(undefined)
    open('/reset-password#token=abc', { confirmReset })
    await screen.findByLabelText('field.new_password')

    fireEvent.change(screen.getByLabelText('field.new_password'), { target: { value: 'NewPassw0rd!' } })
    fireEvent.change(screen.getByLabelText('field.confirm'), { target: { value: 'NewPassw0rd!' } })
    fireEvent.click(screen.getByRole('button', { name: 'action.save' }))

    await waitFor(() => expect(where().path).toBe('/login'))
    expect(where().state).toEqual({ passwordReset: 'done' })
    expect(confirmReset).toHaveBeenCalledWith('abc', 'NewPassw0rd!')
  })
})
