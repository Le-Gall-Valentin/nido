import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AccountPage } from './AccountPage'
import type { IAccountApi } from '../model/IAccountApi'
import type { User } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const mockUseAuth = vi.fn()
vi.mock('@/features/auth', () => ({
  useAuth: (...args: unknown[]) => mockUseAuth(...args),
}))

vi.mock('zustand/react/shallow', () => ({
  useShallow: (fn: unknown) => fn,
}))

vi.mock('@/features/totp', () => ({
  totpApi: {},
}))

// The account api is injected through the page's composition seam (api prop).
const fakeApi: IAccountApi = { updateProfile: vi.fn(), changePassword: vi.fn() }
const renderPage = () => render(<AccountPage api={fakeApi} />)

vi.mock('./ProfileSummaryCard', () => ({
  ProfileSummaryCard: () => <div data-testid="profile-summary" />,
}))

vi.mock('./ProfileEditSection', () => ({
  ProfileEditSection: () => <div data-testid="profile-edit" />,
}))

vi.mock('./TwoFactorSection', () => ({
  TwoFactorSection: () => <div data-testid="totp-section" />,
}))

vi.mock('./PreferencesSection', () => ({
  PreferencesSection: () => <div data-testid="preferences-section" />,
}))

vi.mock('./ChangePasswordSection', () => ({
  ChangePasswordSection: () => <div data-testid="password-section" />,
}))

const BASE_USER: User = {
  id: '1', username: 'alice', email: 'alice@test.com',
  role: 'USER', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

function makeState(overrides: { user?: User | null; logout?: () => Promise<void> } = {}) {
  const state = {
    user: overrides.user !== undefined ? overrides.user : BASE_USER,
    logout: overrides.logout ?? vi.fn().mockResolvedValue(undefined),
    patchUser: vi.fn(),
  }
  mockUseAuth.mockImplementation((selector: (s: typeof state) => unknown) => selector(state))
  return state
}

beforeEach(() => {
  mockUseAuth.mockReset()
})

describe('AccountPage', () => {
  it('returns null when user is null and no error', () => {
    makeState({ user: null })
    const { container } = renderPage()
    expect(container.firstChild).toBeNull()
  })

  it('renders all sections when user is present', () => {
    makeState()
    const { getByTestId } = renderPage()
    expect(getByTestId('profile-summary')).toBeDefined()
    expect(getByTestId('profile-edit')).toBeDefined()
    expect(getByTestId('totp-section')).toBeDefined()
    expect(getByTestId('preferences-section')).toBeDefined()
    expect(getByTestId('password-section')).toBeDefined()
  })

  it('renders logout button', () => {
    makeState()
    const { getByRole } = renderPage()
    expect(getByRole('button', { name: 'action.logout' })).toBeDefined()
  })

  it('calls logout when logout button is clicked', async () => {
    const { logout } = makeState()
    const { getByRole } = renderPage()
    fireEvent.click(getByRole('button', { name: 'action.logout' }))
    await waitFor(() => expect(logout).toHaveBeenCalledOnce())
  })

  it('shows logout error when logout fails and user is still present', async () => {
    makeState({ logout: vi.fn().mockRejectedValue(new Error('network')) })
    const { getByRole, findByRole } = renderPage()
    fireEvent.click(getByRole('button', { name: 'action.logout' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('error.logout_failed')
  })

  it('shows logout error fallback when user is null after failed logout', async () => {
    const logout = vi.fn().mockImplementation(async () => {
      // Simulate: store nulls user in finally, then throws
      makeState({ user: null, logout })
      throw new Error('logout failed')
    })
    makeState({ logout })
    const { getByRole, findByRole } = renderPage()
    fireEvent.click(getByRole('button', { name: 'action.logout' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('error.logout_failed')
  })

  it('prevents double submit while logout is in progress', async () => {
    let resolve!: () => void
    const logout = vi.fn().mockImplementation(
      () => new Promise<void>(r => { resolve = r })
    )
    makeState({ logout })
    const { getByRole } = renderPage()
    fireEvent.click(getByRole('button', { name: 'action.logout' }))
    fireEvent.click(getByRole('button', { name: 'action.logout' }))
    resolve()
    await waitFor(() => expect(logout).toHaveBeenCalledOnce())
  })
})