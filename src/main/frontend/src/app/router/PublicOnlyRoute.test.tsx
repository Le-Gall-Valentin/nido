import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { useAuthGuard } from './useAuthGuard'
import { PublicOnlyRoute } from './PublicOnlyRoute'

vi.mock('./useAuthGuard')

const mockUseAuthGuard = vi.mocked(useAuthGuard)

beforeEach(() => vi.clearAllMocks())

function Landed() {
  const { pathname, search } = useLocation()
  return <div>{`landed on ${pathname}${search}`}</div>
}

function signInFrom(from: { pathname: string; search?: string }) {
  mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, signedOut: false, t: (k: string) => k })
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/login', state: { from } }]}>
      <Routes>
        <Route path="/login" element={<PublicOnlyRoute><div>public</div></PublicOnlyRoute>} />
        <Route path="*" element={<Landed />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('PublicOnlyRoute', () => {
  it('sends a user who just signed in back to the page they were going to', () => {
    expect(signInFrom({ pathname: '/s/space-1/finance', search: '?create=transaction' })
      .getByText('landed on /s/space-1/finance?create=transaction')).toBeDefined()
  })

  it('never follows a remembered page outside the app, nor back to the login page', () => {
    const outside = signInFrom({ pathname: '//elsewhere.example/phish' })
    expect(outside.getByText('landed on /')).toBeDefined()
    outside.unmount()

    expect(signInFrom({ pathname: '/login' }).getByText('landed on /')).toBeDefined()
  })

  it('renders children when not authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, signedOut: false, t: (k: string) => k })
    const { getByText } = render(
      <MemoryRouter>
        <PublicOnlyRoute><div>public</div></PublicOnlyRoute>
      </MemoryRouter>
    )
    expect(getByText('public')).toBeDefined()
  })

  it('redirects and hides children when authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, signedOut: false, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <PublicOnlyRoute><div>public</div></PublicOnlyRoute>
      </MemoryRouter>
    )
    expect(container.textContent).not.toContain('public')
  })

  it('renders spinner while initializing instead of showing children', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: true, isAuthenticated: false, signedOut: false, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <PublicOnlyRoute><div>public</div></PublicOnlyRoute>
      </MemoryRouter>
    )
    expect(container.textContent).not.toContain('public')
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('sends a signed-in user home — where the app opens their space\'s dashboard — not to their profile', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, signedOut: false, t: (k: string) => k })
    const { getByText, queryByText } = render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<PublicOnlyRoute><div>public</div></PublicOnlyRoute>} />
          <Route path="/account" element={<div>on-account</div>} />
          <Route path="*" element={<div>home</div>} />
        </Routes>
      </MemoryRouter>
    )
    expect(getByText('home')).toBeDefined()
    expect(queryByText('on-account')).toBeNull()
  })
})