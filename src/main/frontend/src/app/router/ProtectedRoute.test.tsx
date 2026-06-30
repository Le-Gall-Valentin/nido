import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { useAuthGuard } from './useAuthGuard'
import { ProtectedRoute } from './ProtectedRoute'

vi.mock('./useAuthGuard')

const mockUseAuthGuard = vi.mocked(useAuthGuard)

beforeEach(() => vi.clearAllMocks())

describe('ProtectedRoute', () => {
  it('redirects and hides children when not authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, t: (k: string) => k })
    const { queryByText } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(queryByText('protected')).toBeNull()
  })

  it('renders children when authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, t: (k: string) => k })
    const { getByText } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(getByText('protected')).toBeDefined()
  })

  it('renders spinner while initializing instead of redirecting', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: true, isAuthenticated: false, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(container.textContent).not.toContain('protected')
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('navigates to /login when not authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, t: (k: string) => k })
    const { getByText } = render(
      <MemoryRouter initialEntries={['/protected']}>
        <Routes>
          <Route path="/protected" element={<ProtectedRoute><div>protected</div></ProtectedRoute>} />
          <Route path="/login" element={<div>on-login</div>} />
        </Routes>
      </MemoryRouter>
    )
    expect(getByText('on-login')).toBeDefined()
  })
})