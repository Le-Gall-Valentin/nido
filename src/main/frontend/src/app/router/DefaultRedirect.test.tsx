import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider, activeSpaceStore } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { useAuthGuard } from './useAuthGuard'
import { DefaultRedirect } from './DefaultRedirect'

vi.mock('./useAuthGuard')

const mockUseAuthGuard = vi.mocked(useAuthGuard)

// Node's own global `localStorage` shadows jsdom's real Storage in this test
// environment (see activeSpaceStore.test.ts for the same workaround), so it
// is stubbed with a working in-memory implementation for the duration of
// the test.
class MemoryStorage implements Storage {
  private readonly map = new Map<string, string>()
  get length(): number { return this.map.size }
  clear(): void { this.map.clear() }
  getItem(key: string): string | null { return this.map.has(key) ? this.map.get(key)! : null }
  key(index: number): string | null { return Array.from(this.map.keys())[index] ?? null }
  removeItem(key: string): void { this.map.delete(key) }
  setItem(key: string, value: string): void { this.map.set(key, value) }
}

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}
const FAMILY: SpaceSummary = {
  id: 'space-2', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'ADMIN', memberCount: 4,
}

function fakeApi(spaces: SpaceSummary[] | Promise<never> = [PERSONAL, FAMILY]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockImplementation(() => (spaces instanceof Promise ? spaces : Promise.resolve(spaces))),
    getSpace: vi.fn(),
  }
}

function LocationDisplay() {
  const location = useLocation()
  return <div data-testid="location">{location.pathname}</div>
}

function renderAt(path: string, api: ISpacesApi) {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={api}>
        <MemoryRouter initialEntries={[path]}>
          <LocationDisplay />
          <Routes>
            <Route path="*" element={<DefaultRedirect />} />
            <Route path="/login" element={<div>on-login</div>} />
            <Route path="/account" element={<div>on-account</div>} />
            <Route path="/s/:spaceId" element={<div>on-space</div>} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('localStorage', new MemoryStorage())
  activeSpaceStore.setState({ lastSpaceId: null })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('DefaultRedirect', () => {
  it('shows spinner while initializing instead of redirecting', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: true, isAuthenticated: false, t: (k: string) => k })
    const { container } = renderAt('/', fakeApi())
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('navigates to /login when not authenticated, without waiting on the space list', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: false, t: (k: string) => k })
    const api = fakeApi(new Promise(() => {}))
    const { getByText } = renderAt('/', api)
    expect(getByText('on-login')).toBeDefined()
  })

  it('shows a spinner while the space list is loading, without redirecting', () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, t: (k: string) => k })
    const api = fakeApi(new Promise(() => {}))
    const { container, queryByText } = renderAt('/', api)
    expect(container.querySelector('[role="status"]')).not.toBeNull()
    expect(queryByText('on-account')).toBeNull()
  })

  it('restores the remembered context when it is still in the caller\'s list', async () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, t: (k: string) => k })
    activeSpaceStore.getState().remember('space-2')
    const { findByText, getByTestId } = renderAt('/', fakeApi())
    await findByText('on-space')
    expect(getByTestId('location').textContent).toBe('/s/space-2')
  })

  it('falls back to the personal space when the remembered context is no longer in the list', async () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, t: (k: string) => k })
    activeSpaceStore.getState().remember('space-9-gone')
    const { findByText, getByTestId } = renderAt('/', fakeApi([PERSONAL]))
    await findByText('on-space')
    expect(getByTestId('location').textContent).toBe('/s/personal-1')
    expect(activeSpaceStore.getState().lastSpaceId).toBeNull()
  })

  it('falls back to the personal space when nothing is remembered', async () => {
    mockUseAuthGuard.mockReturnValue({ isInitializing: false, isAuthenticated: true, t: (k: string) => k })
    const { findByText, getByTestId } = renderAt('/', fakeApi([PERSONAL, FAMILY]))
    await findByText('on-space')
    expect(getByTestId('location').textContent).toBe('/s/personal-1')
  })
})
