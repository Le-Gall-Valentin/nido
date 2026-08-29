import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route, useNavigate } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider, activeSpaceStore } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { Sidebar } from './Sidebar'
import type { AuthState, AuthActions } from '@/features/auth/model/authStore'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))

import { useAuth } from '@/features/auth'
const mockUseAuth = vi.mocked(useAuth)

// Node's own global `localStorage` shadows jsdom's real Storage in this test
// environment (see activeSpaceStore.test.ts for the same workaround).
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

function fakeApi(spaces: SpaceSummary[] = [PERSONAL, FAMILY]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockResolvedValue(spaces),
    getSpace: vi.fn(),
  }
}

function withUser(role: 'SUPER_ADMIN' | 'ADMIN' | 'USER' | null) {
  const user = role !== null ? { id: '1', username: 'alice.dupont', role } : null
  mockUseAuth.mockImplementation(
    (selector) => selector({ user } as AuthState & AuthActions)
  )
}

function renderSidebar(path = '/administration/users', open = false, api: ISpacesApi = fakeApi()) {
  const onClose = vi.fn()
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={api}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/s/:spaceId/*" element={<Sidebar open={open} onClose={onClose} />} />
            <Route path="*" element={<Sidebar open={open} onClose={onClose} />} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return { onClose }
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('localStorage', new MemoryStorage())
  activeSpaceStore.setState({ lastSpaceId: null })
})

describe('Sidebar — nav items, always visible', () => {
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

describe('Sidebar — space-scoped items are always reachable, not gated behind entering a space', () => {
  it('shows Membres while on the account page, resolved to the personal space', async () => {
    withUser('USER')
    renderSidebar('/account')

    const link = await screen.findByRole('link', { name: /nav\.members/ })
    expect(link.getAttribute('href')).toBe('/s/personal-1/members')
  })

  it('shows Membres while on the administration page', async () => {
    withUser('SUPER_ADMIN')
    renderSidebar('/administration/users')

    expect(await screen.findByText('nav.members')).toBeDefined()
  })

  it('resolves Membres to the remembered space when one was chosen before', async () => {
    withUser('USER')
    activeSpaceStore.getState().remember('space-2')
    renderSidebar('/account')

    const link = await screen.findByRole('link', { name: /nav\.members/ })
    expect(link.getAttribute('href')).toBe('/s/space-2/members')
  })

  it("keeps Membres pointed at the URL's own space when already inside one", async () => {
    withUser('USER')
    activeSpaceStore.getState().remember('space-2')
    renderSidebar('/s/personal-1/members')

    const link = await screen.findByRole('link', { name: /nav\.members/ })
    expect(link.getAttribute('href')).toBe('/s/personal-1/members')
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
    const queryClient = createTestQueryClient()
    render(
      <QueryClientProvider client={queryClient}>
        <SpacesApiProvider api={fakeApi()}>
          <MemoryRouter initialEntries={['/administration/users']}>
            <Sidebar open onClose={onClose} />
            <NavigateTrigger to="/account" />
          </MemoryRouter>
        </SpacesApiProvider>
      </QueryClientProvider>
    )
    const before = onClose.mock.calls.length
    screen.getByText('go').click()
    expect(onClose.mock.calls.length).toBeGreaterThan(before)
  })
})
