import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Topbar } from './Topbar'
import type { AuthState, AuthActions } from '@/features/auth/model/authStore'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))
vi.mock('@/features/space-switcher', () => ({ SpaceSwitcher: () => <div data-testid="space-switcher" /> }))

import { useAuth } from '@/features/auth'
const mockUseAuth = vi.mocked(useAuth)

function setup({
  user = { id: '1', username: 'alice.dupont', email: 'alice@example.fr', role: 'USER' as const },
  logout = vi.fn().mockResolvedValue(undefined),
  onMenuOpen = vi.fn(),
  onSearchOpen = vi.fn(),
} = {}) {
  mockUseAuth.mockImplementation(
    (selector) => selector({ user, logout } as unknown as AuthState & AuthActions)
  )
  render(
    <MemoryRouter>
      <Topbar onMenuOpen={onMenuOpen} onSearchOpen={onSearchOpen} />
    </MemoryRouter>
  )
  return { onMenuOpen, onSearchOpen, logout }
}

beforeEach(() => vi.clearAllMocks())

describe('Topbar — hamburger', () => {
  it('calls onMenuOpen when the hamburger button is clicked', () => {
    const { onMenuOpen } = setup()
    fireEvent.click(screen.getByLabelText('topbar.menu_label'))
    expect(onMenuOpen).toHaveBeenCalledOnce()
  })
})

describe('Topbar — search', () => {
  it('calls onSearchOpen when the search trigger is clicked', () => {
    const { onSearchOpen } = setup()
    // There are two search buttons (sm+ and mobile), click the first visible one
    const searchButtons = screen.getAllByLabelText('topbar.search_label')
    fireEvent.click(searchButtons[0])
    expect(onSearchOpen).toHaveBeenCalledOnce()
  })
})

describe('Topbar — profile menu', () => {
  it('is closed by default', () => {
    setup()
    expect(screen.queryByText('menu.logout')).toBeNull()
  })

  it('opens on avatar click and shows the user identity', () => {
    setup()
    fireEvent.click(screen.getByLabelText('topbar.profile_label'))
    expect(screen.getByText('alice.dupont')).toBeDefined()
    expect(screen.getByText('alice@example.fr')).toBeDefined()
  })

  it('links to the account page', () => {
    setup()
    fireEvent.click(screen.getByLabelText('topbar.profile_label'))
    const link = screen.getByRole('link', { name: /menu\.settings/ })
    expect(link.getAttribute('href')).toBe('/account')
  })

  it('calls logout when the sign-out item is clicked', async () => {
    const { logout } = setup()
    fireEvent.click(screen.getByLabelText('topbar.profile_label'))
    fireEvent.click(screen.getByText('menu.logout'))
    await waitFor(() => expect(logout).toHaveBeenCalledOnce())
  })

  it('shows an error when logout fails', async () => {
    setup({ logout: vi.fn().mockRejectedValue(new Error('boom')) })
    fireEvent.click(screen.getByLabelText('topbar.profile_label'))
    fireEvent.click(screen.getByText('menu.logout'))
    expect(await screen.findByRole('alert')).toBeDefined()
  })

  it('closes via the backdrop', () => {
    setup()
    fireEvent.click(screen.getByLabelText('topbar.profile_label'))
    fireEvent.click(screen.getByLabelText('topbar.close_menu_label'))
    expect(screen.queryByText('menu.logout')).toBeNull()
  })

  it('closes on Escape', () => {
    setup()
    fireEvent.click(screen.getByLabelText('topbar.profile_label'))
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByText('menu.logout')).toBeNull()
  })

  it('renders no profile menu when user is null', () => {
    setup({ user: null as never })
    expect(screen.queryByLabelText('topbar.profile_label')).toBeNull()
  })

  it('prevents double submit while logout is in progress', async () => {
    let resolve!: () => void
    const logout = vi.fn().mockImplementation(
      () => new Promise<void>((r) => { resolve = r })
    )
    setup({ logout })
    fireEvent.click(screen.getByLabelText('topbar.profile_label'))
    fireEvent.click(screen.getByText('menu.logout'))
    fireEvent.click(screen.getByText('menu.logout'))
    resolve()
    await waitFor(() => expect(logout).toHaveBeenCalledOnce())
  })
})
