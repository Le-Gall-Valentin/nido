import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { useAuthGuard } from './useAuthGuard'
import { ProtectedRoute } from './ProtectedRoute'

vi.mock('./useAuthGuard')

const mockUseAuthGuard = vi.mocked(useAuthGuard)

beforeEach(() => vi.clearAllMocks())

/** The login page, saying which page it was sent from. */
function LoginProbe() {
  const from = (useLocation().state as { from?: { pathname: string; search: string } } | null)?.from
  return <div>{from ? `on-login from ${from.pathname}${from.search}` : 'on-login fresh'}</div>
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/s/:spaceId/finance" element={<ProtectedRoute><div>protected</div></ProtectedRoute>} />
        <Route path="/login" element={<LoginProbe />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('ProtectedRoute', () => {
  it('remembers the page a signed-out visitor was going to, for after the login', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, signedOut: false, t: (k: string) => k })

    expect(renderAt('/s/space-1/finance?create=transaction').getByText('on-login from /s/space-1/finance?create=transaction')).toBeDefined()
  })

  it('forgets it once the user signed themselves out: whoever signs in next starts afresh', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, signedOut: true, t: (k: string) => k })

    expect(renderAt('/s/space-1/finance').getByText('on-login fresh')).toBeDefined()
  })

  it('redirects and hides children when not authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, signedOut: false, t: (k: string) => k })
    const { queryByText } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(queryByText('protected')).toBeNull()
  })

  it('renders children when authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, signedOut: false, t: (k: string) => k })
    const { getByText } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(getByText('protected')).toBeDefined()
  })

  it('renders spinner while initializing instead of redirecting', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: true, isAuthenticated: false, signedOut: false, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <ProtectedRoute><div>protected</div></ProtectedRoute>
      </MemoryRouter>
    )
    expect(container.textContent).not.toContain('protected')
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('navigates to /login when not authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, signedOut: false, t: (k: string) => k })
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