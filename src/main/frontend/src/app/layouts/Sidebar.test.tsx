import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, useNavigate } from 'react-router-dom'
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
      <Sidebar open={open} onClose={onClose} />
    </MemoryRouter>
  )
  return { onClose }
}

beforeEach(() => vi.clearAllMocks())

describe('Sidebar — admin section visibility', () => {
  it('hides administration section for USER role', () => {
    withUser('USER')
    renderSidebar()
    expect(screen.queryByText('nav.section.admin')).toBeNull()
    expect(screen.queryByText('nav.users')).toBeNull()
  })

  it('hides administration section when user is null', () => {
    withUser(null)
    renderSidebar()
    expect(screen.queryByText('nav.section.admin')).toBeNull()
  })

  it('shows administration section for ADMIN role', () => {
    withUser('ADMIN')
    renderSidebar()
    expect(screen.getByText('nav.section.admin')).toBeDefined()
    expect(screen.getByText('nav.users')).toBeDefined()
  })

  it('shows administration section for SUPER_ADMIN role', () => {
    withUser('SUPER_ADMIN')
    renderSidebar()
    expect(screen.getByText('nav.section.admin')).toBeDefined()
    expect(screen.getByText('nav.users')).toBeDefined()
  })
})

describe('Sidebar — user footer', () => {
  it('shows initials from username split on dots', () => {
    withUser('USER') // username = 'alice.dupont'
    renderSidebar()
    expect(screen.getByText('AD')).toBeDefined()
  })

  it('shows the correct role label key', () => {
    withUser('ADMIN')
    renderSidebar()
    expect(screen.getByText('user.role.ADMIN')).toBeDefined()
  })

  it('shows SUPER_ADMIN role label key', () => {
    withUser('SUPER_ADMIN')
    renderSidebar()
    expect(screen.getByText('user.role.SUPER_ADMIN')).toBeDefined()
  })

  it('shows USER role label key', () => {
    withUser('USER')
    renderSidebar()
    expect(screen.getByText('user.role.USER')).toBeDefined()
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