import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { useAuthGuard } from './useAuthGuard'
import { PublicOnlyRoute } from './PublicOnlyRoute'

vi.mock('./useAuthGuard')

const mockUseAuthGuard = vi.mocked(useAuthGuard)

beforeEach(() => vi.clearAllMocks())

describe('PublicOnlyRoute', () => {
  it('renders children when not authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, t: (k: string) => k })
    const { getByText } = render(
      <MemoryRouter>
        <PublicOnlyRoute><div>public</div></PublicOnlyRoute>
      </MemoryRouter>
    )
    expect(getByText('public')).toBeDefined()
  })

  it('redirects and hides children when authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <PublicOnlyRoute><div>public</div></PublicOnlyRoute>
      </MemoryRouter>
    )
    expect(container.textContent).not.toContain('public')
  })

  it('renders spinner while initializing instead of showing children', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: true, isAuthenticated: false, t: (k: string) => k })
    const { container } = render(
      <MemoryRouter>
        <PublicOnlyRoute><div>public</div></PublicOnlyRoute>
      </MemoryRouter>
    )
    expect(container.textContent).not.toContain('public')
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('navigates to /account when authenticated', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, t: (k: string) => k })
    const { getByText } = render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<PublicOnlyRoute><div>public</div></PublicOnlyRoute>} />
          <Route path="/account" element={<div>on-account</div>} />
        </Routes>
      </MemoryRouter>
    )
    expect(getByText('on-account')).toBeDefined()
  })
})