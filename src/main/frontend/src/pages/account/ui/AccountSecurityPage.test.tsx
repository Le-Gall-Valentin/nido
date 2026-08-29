import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AccountSecurityPage } from './AccountSecurityPage'
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

const fakeApi: IAccountApi = { updateProfile: vi.fn(), changePassword: vi.fn() }
const renderPage = () => render(<AccountSecurityPage api={fakeApi} />)

vi.mock('./TwoFactorSection', () => ({
  TwoFactorSection: () => <div data-testid="totp-section" />,
}))

vi.mock('./ChangePasswordSection', () => ({
  ChangePasswordSection: () => <div data-testid="password-section" />,
}))

const BASE_USER: User = {
  id: '1', username: 'alice', email: 'alice@test.com',
  role: 'USER', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

function makeState(overrides: { user?: User | null } = {}) {
  const state = {
    user: overrides.user !== undefined ? overrides.user : BASE_USER,
    patchUser: vi.fn(),
  }
  mockUseAuth.mockImplementation((selector: (s: typeof state) => unknown) => selector(state))
  return state
}

beforeEach(() => {
  mockUseAuth.mockReset()
})

describe('AccountSecurityPage', () => {
  it('returns null when user is null', () => {
    makeState({ user: null })
    const { container } = renderPage()
    expect(container.firstChild).toBeNull()
  })

  it('renders the security sections only', () => {
    makeState()
    const { getByTestId, queryByTestId } = renderPage()
    expect(getByTestId('totp-section')).toBeDefined()
    expect(getByTestId('password-section')).toBeDefined()
    expect(queryByTestId('profile-summary')).toBeNull()
    expect(queryByTestId('preferences-section')).toBeNull()
  })

  it('renders the page heading with kicker, title and subtitle', () => {
    makeState()
    const { getByText, getByRole } = renderPage()
    expect(getByText('kicker')).toBeDefined()
    expect(getByRole('heading', { level: 1, name: 'pages.security.title' })).toBeDefined()
    expect(getByText('pages.security.subtitle')).toBeDefined()
  })
})
