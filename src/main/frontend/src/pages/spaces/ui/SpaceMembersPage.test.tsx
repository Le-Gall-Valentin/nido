import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import type { ISpacesPageApi } from '../model/ISpacesPageApi'
import { SpaceMembersPage } from './SpaceMembersPage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k), i18n: { language: 'en' } }),
}))

vi.mock('@/features/auth', () => ({ useAuth: vi.fn((selector) => selector({ user: { id: 'me-1' } })) }))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return { ...actual, useNavigate: () => mockNavigate }
})

const SHARED: SpaceSummary = { id: 's-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'OWNER', memberCount: 1 }

function fakeSpacesApi(): ISpacesApi {
  return { listMySpaces: vi.fn().mockResolvedValue([SHARED]), getSpace: vi.fn() }
}

function fakePageApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn().mockResolvedValue({ ...SHARED, description: null }),
    listMembers: vi.fn().mockResolvedValue([{ userId: 'me-1', username: 'alice', email: 'a@test.com', role: 'OWNER', joinedAt: '2024-01-01T00:00:00Z' }]),
    listInvitations: vi.fn().mockResolvedValue([]),
    listReceivedInvitations: vi.fn(),
    createSpace: vi.fn(), updateSpace: vi.fn(), deleteSpace: vi.fn().mockResolvedValue(undefined),
    changeMemberRole: vi.fn(), removeMember: vi.fn(), transferOwnership: vi.fn(),
    leaveSpace: vi.fn(), inviteMember: vi.fn(), revokeInvitation: vi.fn(), acceptInvitation: vi.fn(),
    ...overrides,
  }
}

function renderAt(spaceId: string, pageApi: ISpacesPageApi) {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={fakeSpacesApi()}>
        <MemoryRouter initialEntries={[`/s/${spaceId}/members`]}>
          <Routes>
            <Route path="/s/:spaceId/members" element={<SpaceMembersPage api={pageApi} />} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
}

beforeEach(() => vi.clearAllMocks())

describe('SpaceMembersPage', () => {
  it('takes its context from the URL and renders that space', async () => {
    renderAt('s-1', fakePageApi())
    expect(await screen.findByText('Chez nous')).toBeDefined()
  })

  it('navigates to the groups page after a successful delete', async () => {
    const pageApi = fakePageApi()
    renderAt('s-1', pageApi)
    fireEvent.click(await screen.findByText('actions.delete'))
    fireEvent.click(screen.getByText('delete.submit'))
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/spaces'))
  })
})
