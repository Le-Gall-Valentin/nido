import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary, ReceivedInvitation } from '@/entities/space'
import { SpacesPage } from '@/pages/spaces'
import { SpaceRoute } from './SpaceRoute'

// pages/spaces's ISpacesPageApi is a private, unexported part of that slice
// from this (app-layer) test's point of view — derived structurally from
// SpacesPage's own prop instead of a boundary-violating deep import.
type SpacesPageApi = NonNullable<NonNullable<Parameters<typeof SpacesPage>[0]>['api']>

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k),
    i18n: { language: 'en' },
  }),
}))

const PERSONAL: SpaceSummary = {
  id: 'personal-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}
const GROUP: SpaceSummary = {
  id: 'group-9', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 3,
}
const INVITATION: ReceivedInvitation = {
  invitationId: 'i-1', spaceId: 'group-9', spaceName: 'La Famille', spaceAccent: '#c17a5c', spaceGlyph: '🏡',
  role: 'MEMBER', expiresAt: '2999-01-01T00:00:00Z',
}

function LocationDisplay() {
  const location = useLocation()
  return <div data-testid="location">{location.pathname}</div>
}

/**
 * Reproduces the reported journey end-to-end: /spaces -> accept an
 * invitation -> land on the joined context's members page, and stay there
 * — never bounced to the personal space by a route guard racing the
 * invalidation the acceptance triggers.
 */
describe('accepting an invitation', () => {
  it('lands the user on the joined context, not back on their personal space', async () => {
    let joined = false
    const spacesApi: ISpacesApi = {
      // The post-acceptance refetch carries a small real delay: with a
      // fire-and-forget invalidation, navigation would run before it settles
      // and the guard would still see the pre-join list.
      listMySpaces: vi.fn().mockImplementation(
        () => joined
          ? new Promise((resolve) => setTimeout(() => resolve([PERSONAL, GROUP]), 20))
          : Promise.resolve([PERSONAL])
      ),
      getSpace: vi.fn(),
    }
    const pageApi: SpacesPageApi = {
      getSpaceDetail: vi.fn(), listMembers: vi.fn(), listInvitations: vi.fn(),
      listReceivedInvitations: vi.fn().mockResolvedValue([INVITATION]),
      createSpace: vi.fn(), updateSpace: vi.fn(), deleteSpace: vi.fn(),
      changeMemberRole: vi.fn(), removeMember: vi.fn(), transferOwnership: vi.fn(),
      leaveSpace: vi.fn(), inviteMember: vi.fn(), revokeInvitation: vi.fn(),
      acceptInvitation: vi.fn().mockImplementation(async () => {
        joined = true
        return { spaceId: 'group-9' }
      }),
    }
    const queryClient = createTestQueryClient()

    render(
      <QueryClientProvider client={queryClient}>
        <SpacesApiProvider api={spacesApi}>
          <MemoryRouter initialEntries={['/spaces']}>
            <LocationDisplay />
            <Routes>
              <Route path="/spaces" element={<SpacesPage api={pageApi} />} />
              <Route
                path="/s/:spaceId/members"
                element={<SpaceRoute><div>scoped-content</div></SpaceRoute>}
              />
            </Routes>
          </MemoryRouter>
        </SpacesApiProvider>
      </QueryClientProvider>
    )

    fireEvent.click(await screen.findByText('received.action_accept'))

    await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/s/group-9/members'))
    expect(await screen.findByText('scoped-content')).toBeDefined()
    // Never bounced through the personal space on the way there.
    expect(screen.getByTestId('location').textContent).toBe('/s/group-9/members')
  })
})
