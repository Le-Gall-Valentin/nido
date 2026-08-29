import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AccountProfilePage } from './AccountProfilePage'
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

const fakeApi: IAccountApi = { updateProfile: vi.fn(), changePassword: vi.fn() }
const renderPage = () => render(<AccountProfilePage api={fakeApi} />)

vi.mock('./ProfileSummaryCard', () => ({
  ProfileSummaryCard: () => <div data-testid="profile-summary" />,
}))

vi.mock('./ProfileEditSection', () => ({
  ProfileEditSection: () => <div data-testid="profile-edit" />,
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

describe('AccountProfilePage', () => {
  it('returns null when user is null', () => {
    makeState({ user: null })
    const { container } = renderPage()
    expect(container.firstChild).toBeNull()
  })

  it('renders the profile sections only', () => {
    makeState()
    const { getByTestId, queryByTestId } = renderPage()
    expect(getByTestId('profile-summary')).toBeDefined()
    expect(getByTestId('profile-edit')).toBeDefined()
    expect(queryByTestId('totp-section')).toBeNull()
    expect(queryByTestId('preferences-section')).toBeNull()
    expect(queryByTestId('password-section')).toBeNull()
  })

  it('renders the page heading with kicker, title and subtitle', () => {
    makeState()
    const { getByText, getByRole } = renderPage()
    expect(getByText('kicker')).toBeDefined()
    expect(getByRole('heading', { level: 1, name: 'pages.profile.title' })).toBeDefined()
    expect(getByText('pages.profile.subtitle')).toBeDefined()
  })
})
