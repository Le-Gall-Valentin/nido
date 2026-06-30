import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import type { AuthState, AuthActions } from '@/features/auth/model/authStore'
import { AdminRoute } from './AdminRoute'

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))

const mockUseAuth = vi.mocked(useAuth)

function withUser(role: 'SUPER_ADMIN' | 'ADMIN' | 'USER' | null) {
  const user =
    role !== null ? { id: '1', username: 'test', role } : null
  mockUseAuth.mockImplementation(
    (selector) => selector({ user } as AuthState & AuthActions)
  )
}

beforeEach(() => vi.clearAllMocks())

describe('AdminRoute', () => {
  it('renders children for ADMIN', () => {
    withUser('ADMIN')
    const { getByText } = render(
      <MemoryRouter>
        <AdminRoute><div>admin-content</div></AdminRoute>
      </MemoryRouter>
    )
    expect(getByText('admin-content')).toBeDefined()
  })

  it('renders children for SUPER_ADMIN', () => {
    withUser('SUPER_ADMIN')
    const { getByText } = render(
      <MemoryRouter>
        <AdminRoute><div>admin-content</div></AdminRoute>
      </MemoryRouter>
    )
    expect(getByText('admin-content')).toBeDefined()
  })

  it('redirects USER to /account', () => {
    withUser('USER')
    const { getByText } = render(
      <MemoryRouter initialEntries={['/admin/users']}>
        <Routes>
          <Route path="/admin/users" element={<AdminRoute><div>admin-content</div></AdminRoute>} />
          <Route path="/account" element={<div>on-account</div>} />
        </Routes>
      </MemoryRouter>
    )
    expect(getByText('on-account')).toBeDefined()
  })

  it('redirects unauthenticated user to /account', () => {
    withUser(null)
    const { getByText } = render(
      <MemoryRouter initialEntries={['/admin/users']}>
        <Routes>
          <Route path="/admin/users" element={<AdminRoute><div>admin-content</div></AdminRoute>} />
          <Route path="/account" element={<div>on-account</div>} />
        </Routes>
      </MemoryRouter>
    )
    expect(getByText('on-account')).toBeDefined()
  })

  it('hides children when user is USER', () => {
    withUser('USER')
    const { queryByText } = render(
      <MemoryRouter>
        <AdminRoute><div>admin-content</div></AdminRoute>
      </MemoryRouter>
    )
    expect(queryByText('admin-content')).toBeNull()
  })
})