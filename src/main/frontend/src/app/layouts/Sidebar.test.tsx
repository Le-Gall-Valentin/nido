import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route, useNavigate } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import type { AuthState, AuthActions } from '@/features/auth/model/authStore'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))

import { useAuth } from '@/features/auth'
const mockUseAuth = vi.mocked(useAuth)

function withUser(role: 'SUPER_ADMIN' | 'ADMIN' | 'USER' | null) {
  const user = role !== null ? { id: '1', username: 'alice.dupont', role } : null
  mockUseAuth.mockImplementation(
    (selector) => selector({ user } as AuthState & AuthActions)
  )
}

function renderSidebar(path = '/administration/users', open = false) {
  const onClose = vi.fn()
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/s/:spaceId/*" element={<Sidebar open={open} onClose={onClose} />} />
        <Route path="*" element={<Sidebar open={open} onClose={onClose} />} />
      </Routes>
    </MemoryRouter>
  )
  return { onClose }
}

beforeEach(() => vi.clearAllMocks())

describe('Sidebar — nav items', () => {
  it('always shows the settings entry', () => {
    withUser('USER')
    renderSidebar()
    expect(screen.getByText('nav.settings')).toBeDefined()
  })

  it('always shows the groups entry', () => {
    withUser('USER')
    renderSidebar()
    expect(screen.getByText('nav.groups')).toBeDefined()
  })

  it('hides the administration entry for USER role', () => {
    withUser('USER')
    renderSidebar()
    expect(screen.queryByText('nav.administration')).toBeNull()
  })

  it('hides the administration entry when user is null', () => {
    withUser(null)
    renderSidebar()
    expect(screen.queryByText('nav.administration')).toBeNull()
  })

  it('shows the administration entry for ADMIN role', () => {
    withUser('ADMIN')
    renderSidebar()
    expect(screen.getByText('nav.administration')).toBeDefined()
  })

  it('shows the administration entry for SUPER_ADMIN role', () => {
    withUser('SUPER_ADMIN')
    renderSidebar()
    expect(screen.getByText('nav.administration')).toBeDefined()
  })
})

describe('Sidebar — brand', () => {
  it('renders the brand name linking to the account page', () => {
    withUser('USER')
    renderSidebar()
    const link = screen.getByRole('link', { name: /brand/ })
    expect(link.getAttribute('href')).toBe('/account')
  })
})

function NavigateTrigger({ to }: { to: string }) {
  const nav = useNavigate()
  return <button onClick={() => { act(() => { nav(to) }) }}>go</button>
}

describe('Sidebar — onClose behaviour', () => {
  it('does not call onClose on initial mount', () => {
    withUser('USER')
    const { onClose } = renderSidebar()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('calls onClose when the route changes', async () => {
    withUser('USER')
    const onClose = vi.fn()
    render(
      <MemoryRouter initialEntries={['/administration/users']}>
        <Sidebar open onClose={onClose} />
        <NavigateTrigger to="/account" />
      </MemoryRouter>
    )
    const before = onClose.mock.calls.length
    screen.getByText('go').click()
    expect(onClose.mock.calls.length).toBeGreaterThan(before)
  })
})

describe('Sidebar — space-scoped nav', () => {
  it('shows the space nav items when the route carries a spaceId', () => {
    withUser('USER')
    renderSidebar('/s/space-1/members')
    expect(screen.getByText('nav.members')).toBeDefined()
  })

  it('hides the global nav entries when the route carries a spaceId', () => {
    withUser('USER')
    renderSidebar('/s/space-1/members')
    expect(screen.queryByText('nav.groups')).toBeNull()
    expect(screen.queryByText('nav.settings')).toBeNull()
    expect(screen.queryByText('nav.administration')).toBeNull()
  })

  it('shows a back-to-groups link pointing at /spaces', () => {
    withUser('USER')
    renderSidebar('/s/space-1/members')
    const link = screen.getByRole('link', { name: /back_to_groups/ })
    expect(link.getAttribute('href')).toBe('/spaces')
  })

  it('shows the global nav and no back link outside a space route', () => {
    withUser('USER')
    renderSidebar('/account')
    expect(screen.getByText('nav.groups')).toBeDefined()
    expect(screen.queryByText('nav.members')).toBeNull()
    expect(screen.queryByRole('link', { name: /back_to_groups/ })).toBeNull()
  })
})
