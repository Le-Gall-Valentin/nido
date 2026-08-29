import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AccountPreferencesPage } from './AccountPreferencesPage'
import type { User } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const mockUseAuth = vi.fn()
vi.mock('@/features/auth', () => ({
  useAuth: (...args: unknown[]) => mockUseAuth(...args),
}))

vi.mock('./PreferencesSection', () => ({
  PreferencesSection: () => <div data-testid="preferences-section" />,
}))

const BASE_USER: User = {
  id: '1', username: 'alice', email: 'alice@test.com',
  role: 'USER', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

function makeState(user: User | null = BASE_USER) {
  mockUseAuth.mockImplementation((selector: (s: { user: User | null }) => unknown) => selector({ user }))
}

beforeEach(() => {
  mockUseAuth.mockReset()
})

describe('AccountPreferencesPage', () => {
  it('returns null when user is null', () => {
    makeState(null)
    const { container } = render(<AccountPreferencesPage />)
    expect(container.firstChild).toBeNull()
  })

  it('renders the preferences section', () => {
    makeState()
    const { getByTestId } = render(<AccountPreferencesPage />)
    expect(getByTestId('preferences-section')).toBeDefined()
  })

  it('renders the page heading with kicker, title and subtitle', () => {
    makeState()
    const { getByText, getByRole } = render(<AccountPreferencesPage />)
    expect(getByText('kicker')).toBeDefined()
    expect(getByRole('heading', { level: 1, name: 'pages.preferences.title' })).toBeDefined()
    expect(getByText('pages.preferences.subtitle')).toBeDefined()
  })
})
