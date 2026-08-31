import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
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

function renderSidebar(path = '/administration/users', api: ISpacesApi = fakeApi(), hasPendingInvitations = false) {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={api}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/s/:spaceId/*" element={<Sidebar hasPendingInvitations={hasPendingInvitations} />} />
            <Route path="*" element={<Sidebar hasPendingInvitations={hasPendingInvitations} />} />
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

describe('Sidebar — Membres et groupes is a single entry, matching the mockup', () => {
  // The mockup has exactly one "Membres & groupes" nav item — no separate
  // per-space "Membres" item alongside it (drilling into a group from
  // /spaces already reaches its members page). A prior fix mistakenly
  // added a second "Membres" item; this locks in that there is only one.
  it('shows exactly one groups-related entry, on the account page', async () => {
    withUser('USER')
    renderSidebar('/account')
    await screen.findByText('nav.groups')

    expect(screen.queryByText('nav.members')).toBeNull()
  })

  it('shows exactly one groups-related entry, while inside a space', async () => {
    withUser('USER')
    renderSidebar('/s/personal-1/members')
    await screen.findByText('nav.groups')

    expect(screen.queryByText('nav.members')).toBeNull()
  })
})

describe('Sidebar — Paramètres sub-navigation', () => {
  it("shows the 3 real sub-categories, not the mockup's unimplemented Notifications", () => {
    withUser('USER')
    renderSidebar('/account/security')

    expect(screen.getByText('nav.settings_profile')).toBeDefined()
    expect(screen.getByText('nav.settings_security')).toBeDefined()
    expect(screen.getByText('nav.settings_preferences')).toBeDefined()
    expect(screen.queryByText(/notification/i)).toBeNull()
  })

  it('keeps "Paramètres" highlighted while on a sibling sub-page (Sécurité), not just its own link', () => {
    withUser('USER')
    renderSidebar('/account/security')

    const parentLink = screen.getByRole('link', { name: /nav\.settings$/ })
    expect(parentLink.className).toContain('bg-accent-dim')
  })
})

describe('Sidebar — pending invitations badge', () => {
  it('shows a badge on the groups entry when there are pending invitations', () => {
    withUser('USER')
    renderSidebar('/administration/users', fakeApi(), true)
    expect(screen.getByText('nav.pending_invitations')).toBeDefined()
  })

  it('shows no badge when there are no pending invitations', () => {
    withUser('USER')
    renderSidebar('/administration/users', fakeApi(), false)
    expect(screen.queryByText('nav.pending_invitations')).toBeNull()
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
