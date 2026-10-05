import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import type { AuthState, AuthActions } from '@/features/auth/model/authStore'
import { SuperAdminRoute } from './SuperAdminRoute'

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))

const mockUseAuth = vi.mocked(useAuth)

function withUser(role: 'SUPER_ADMIN' | 'ADMIN' | 'USER' | null) {
  const user = role !== null ? { id: '1', username: 'test', role } : null
  mockUseAuth.mockImplementation((selector) => selector({ user } as AuthState & AuthActions))
}

function openSettings() {
  return render(
    <MemoryRouter initialEntries={['/administration/settings']}>
      <Routes>
        <Route path="/administration/settings" element={<SuperAdminRoute><div>settings-content</div></SuperAdminRoute>} />
        <Route path="/account" element={<div>on-account</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => vi.clearAllMocks())

describe('SuperAdminRoute', () => {
  it('renders children for SUPER_ADMIN', () => {
    withUser('SUPER_ADMIN')
    expect(openSettings().getByText('settings-content')).toBeDefined()
  })

  it('sends an ADMIN back to the account: the instance settings concern the whole installation', () => {
    withUser('ADMIN')
    expect(openSettings().getByText('on-account')).toBeDefined()
  })

  it('sends anyone else back to the account', () => {
    withUser('USER')
    expect(openSettings().getByText('on-account')).toBeDefined()
    withUser(null)
    expect(openSettings().getAllByText('on-account').length).toBeGreaterThan(0)
  })
})
