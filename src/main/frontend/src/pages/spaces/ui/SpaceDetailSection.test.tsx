import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { SpaceDetail, SpaceMember, SpaceInvitation } from '@/entities/space'
import type { ISpacesPageApi } from '../model/ISpacesPageApi'
import { SpacesPageApiProvider } from '../model/spacesPageApiContext'
import { SpaceDetailSection } from './SpaceDetailSection'
import { SpaceNotAccessibleError, SPACES_QUERY_KEY } from '@/features/space-switcher'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k), i18n: { language: 'en' } }),
}))

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))
import { useAuth } from '@/features/auth'
const mockUseAuth = vi.mocked(useAuth)

const ME = { id: 'me-1' }

function withRole(role: SpaceDetail['myRole']) {
  mockUseAuth.mockImplementation((selector) => selector({ user: ME } as never))
  return role
}

const SHARED_DETAIL: SpaceDetail = {
  id: 's-1', type: 'SHARED', name: 'Chez nous', description: 'Notre appartement',
  accent: '#c17a5c', glyph: '🏡', myRole: 'OWNER', memberCount: 2,
}
const PERSONAL_DETAIL: SpaceDetail = {
  id: 'p-1', type: 'PERSONAL', name: 'Alice', description: null,
  accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1,
}
const MEMBERS: SpaceMember[] = [
  { userId: 'me-1', username: 'alice', email: 'alice@test.com', role: 'OWNER', joinedAt: '2024-01-01T00:00:00Z' },
  { userId: 'u-2', username: 'bob', email: 'bob@test.com', role: 'MEMBER', joinedAt: '2024-01-02T00:00:00Z' },
]
const INVITATIONS: SpaceInvitation[] = [
  { id: 'i-1', email: 'carol@test.com', role: 'MEMBER', code: 'NIDO-ABC', status: 'PENDING', expiresAt: '2999-01-01T00:00:00Z', createdAt: '2024-01-01T00:00:00Z' },
]

function fakeApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn().mockResolvedValue(SHARED_DETAIL),
    listMembers: vi.fn().mockResolvedValue(MEMBERS),
    listInvitations: vi.fn().mockResolvedValue(INVITATIONS),
    listReceivedInvitations: vi.fn(),
    createSpace: vi.fn(),
    updateSpace: vi.fn(),
    deleteSpace: vi.fn().mockResolvedValue(undefined),
    changeMemberRole: vi.fn().mockResolvedValue(undefined),
    removeMember: vi.fn().mockResolvedValue(undefined),
    transferOwnership: vi.fn().mockResolvedValue(undefined),
    leaveSpace: vi.fn().mockResolvedValue(undefined),
    inviteMember: vi.fn().mockResolvedValue(INVITATIONS[0]),
    revokeInvitation: vi.fn().mockResolvedValue(undefined),
    acceptInvitation: vi.fn(),
    ...overrides,
  }
}

function renderSection(
  api: ISpacesPageApi,
  spaceId = 's-1',
  onLeft = vi.fn(),
  onDeleted = vi.fn(),
  queryClient: QueryClient = createTestQueryClient()
) {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SpacesPageApiProvider api={api}>{children}</SpacesPageApiProvider>
    </QueryClientProvider>
  )
  return {
    ...render(<SpaceDetailSection spaceId={spaceId} onLeft={onLeft} onDeleted={onDeleted} />, { wrapper }),
    onLeft,
    onDeleted,
    queryClient,
  }
}

beforeEach(() => { vi.clearAllMocks(); withRole('OWNER') })

describe('SpaceDetailSection — loading and error', () => {
  it('shows a spinner while the detail is loading', () => {
    const api = fakeApi({ getSpaceDetail: vi.fn().mockReturnValue(new Promise(() => {})) })
    renderSection(api)
    expect(screen.getByRole('status')).toBeDefined()
  })

  it('shows an error message, never a permission message, on a 404', async () => {
    const api = fakeApi({ getSpaceDetail: vi.fn().mockRejectedValue(new SpaceNotAccessibleError()) })
    renderSection(api)
    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('errors.not_accessible')
    expect(alert.textContent).not.toContain('insufficient_role')
  })

  it('invalidates ["spaces"] on a 404 so the route guard takes over instead of stranding the user', async () => {
    const api = fakeApi({ getSpaceDetail: vi.fn().mockRejectedValue(new SpaceNotAccessibleError()) })
    const queryClient = createTestQueryClient()
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries')
    renderSection(api, 's-1', vi.fn(), vi.fn(), queryClient)

    await screen.findByRole('alert')
    expect(invalidate).toHaveBeenCalledWith({ queryKey: [SPACES_QUERY_KEY] })
  })
})

describe('SpaceDetailSection — identity', () => {
  it('renders the name, description and my role once loaded', async () => {
    renderSection(fakeApi())
    expect(await screen.findByText('Chez nous')).toBeDefined()
    expect(screen.getByText('Notre appartement')).toBeDefined()
  })
})

describe('SpaceDetailSection — action visibility by role', () => {
  it('shows Invite for a manager (OWNER) on a shared space', async () => {
    withRole('OWNER')
    renderSection(fakeApi())
    expect(await screen.findByText('actions.invite')).toBeDefined()
  })

  it('hides Invite for a MEMBER', async () => {
    withRole('MEMBER')
    renderSection(fakeApi({ getSpaceDetail: vi.fn().mockResolvedValue({ ...SHARED_DETAIL, myRole: 'MEMBER' }) }))
    await screen.findByText('Chez nous')
    expect(screen.queryByText('actions.invite')).toBeNull()
  })

  it('shows Edit (the personalisation entry point) for a manager on a shared space', async () => {
    withRole('OWNER')
    renderSection(fakeApi())
    expect(await screen.findByText('actions.edit')).toBeDefined()
  })

  it('hides Edit for a MEMBER', async () => {
    withRole('MEMBER')
    renderSection(fakeApi({ getSpaceDetail: vi.fn().mockResolvedValue({ ...SHARED_DETAIL, myRole: 'MEMBER' }) }))
    await screen.findByText('Chez nous')
    expect(screen.queryByText('actions.edit')).toBeNull()
  })

  it('shows Leave for a non-owner on a shared space', async () => {
    renderSection(fakeApi({ getSpaceDetail: vi.fn().mockResolvedValue({ ...SHARED_DETAIL, myRole: 'ADMIN' }) }))
    expect(await screen.findByText('actions.leave')).toBeDefined()
  })

  it('hides Leave for the owner', async () => {
    renderSection(fakeApi())
    await screen.findByText('Chez nous')
    expect(screen.queryByText('actions.leave')).toBeNull()
  })

  it('shows Delete for the owner of a shared space', async () => {
    renderSection(fakeApi())
    expect(await screen.findByText('actions.delete')).toBeDefined()
  })

  it('hides every manage action on the personal space even though I am its OWNER', async () => {
    renderSection(fakeApi({
      getSpaceDetail: vi.fn().mockResolvedValue(PERSONAL_DETAIL),
      listMembers: vi.fn().mockResolvedValue([{ userId: 'me-1', username: 'alice', email: 'a@test.com', role: 'OWNER', joinedAt: '2024-01-01T00:00:00Z' }]),
    }))
    await screen.findByText('Alice')
    expect(screen.queryByText('actions.invite')).toBeNull()
    expect(screen.queryByText('actions.leave')).toBeNull()
    expect(screen.queryByText('actions.delete')).toBeNull()
    expect(screen.queryByText('actions.edit')).toBeNull()
  })
})

describe('SpaceDetailSection — invitations visibility', () => {
  it('fetches and shows invitations for a manager', async () => {
    const api = fakeApi()
    renderSection(api)
    expect(await screen.findByText('carol@test.com')).toBeDefined()
    expect(api.listInvitations).toHaveBeenCalledWith('s-1')
  })

  it('does not fetch invitations for a non-manager', async () => {
    const api = fakeApi({ getSpaceDetail: vi.fn().mockResolvedValue({ ...SHARED_DETAIL, myRole: 'MEMBER' }) })
    renderSection(api)
    await screen.findByText('Chez nous')
    expect(api.listInvitations).not.toHaveBeenCalled()
  })
})

describe('SpaceDetailSection — modals and navigation', () => {
  it('opens the invite modal and navigates nowhere on close', async () => {
    renderSection(fakeApi())
    fireEvent.click(await screen.findByText('actions.invite'))
    expect(screen.getAllByText('invite.title').length).toBeGreaterThan(0)
  })

  it('leaving successfully calls onLeft', async () => {
    const api = fakeApi({ getSpaceDetail: vi.fn().mockResolvedValue({ ...SHARED_DETAIL, myRole: 'ADMIN' }) })
    const { onLeft } = renderSection(api)
    fireEvent.click(await screen.findByText('actions.leave'))
    fireEvent.click(screen.getByText('leave.submit'))
    await waitFor(() => expect(onLeft).toHaveBeenCalledOnce())
  })

  it('deleting successfully calls onDeleted', async () => {
    const api = fakeApi()
    const { onDeleted } = renderSection(api)
    fireEvent.click(await screen.findByText('actions.delete'))
    fireEvent.click(screen.getByText('delete.submit'))
    await waitFor(() => expect(onDeleted).toHaveBeenCalledOnce())
  })
})

describe('SpaceDetailSection — member actions', () => {
  it('shows an alert when removing a member fails', async () => {
    const api = fakeApi({ removeMember: vi.fn().mockRejectedValue(new Error('boom')) })
    renderSection(api)
    await screen.findByText('bob')
    fireEvent.click(screen.getByLabelText(/action_remove/))
    await waitFor(() => expect(screen.getByRole('alert')).toBeDefined())
  })
})
