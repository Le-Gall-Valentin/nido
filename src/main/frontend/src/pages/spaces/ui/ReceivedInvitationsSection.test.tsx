import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { ReceivedInvitation } from '@/entities/space'
import type { ISpacesPageApi } from '../model/ISpacesPageApi'
import { SpacesPageApiProvider } from '../model/spacesPageApiContext'
import { ReceivedInvitationsSection } from './ReceivedInvitationsSection'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k), i18n: { language: 'en' } }),
}))

const INVITATION: ReceivedInvitation = {
  invitationId: 'i-1', spaceId: 's-2', spaceName: 'Chez Bob', spaceAccent: '#4a7fa0', spaceGlyph: '🌿',
  role: 'MEMBER', expiresAt: '2999-01-01T00:00:00Z',
}

function fakeApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn(), listMembers: vi.fn(), listInvitations: vi.fn(),
    listReceivedInvitations: vi.fn().mockResolvedValue([INVITATION]),
    createSpace: vi.fn(), updateSpace: vi.fn(), deleteSpace: vi.fn(),
    changeMemberRole: vi.fn(), removeMember: vi.fn(), transferOwnership: vi.fn(),
    leaveSpace: vi.fn(), inviteMember: vi.fn(), revokeInvitation: vi.fn(),
    acceptInvitation: vi.fn().mockResolvedValue({ spaceId: 's-2' }),
    ...overrides,
  }
}

function renderWith(api: ISpacesPageApi, onAccepted = vi.fn(), queryClient = createTestQueryClient()) {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SpacesPageApiProvider api={api}>{children}</SpacesPageApiProvider>
    </QueryClientProvider>
  )
  return { ...render(<ReceivedInvitationsSection onAccepted={onAccepted} />, { wrapper }), onAccepted, queryClient }
}

beforeEach(() => vi.clearAllMocks())

describe('ReceivedInvitationsSection', () => {
  it('renders nothing when there are no received invitations', async () => {
    const { container } = renderWith(fakeApi({ listReceivedInvitations: vi.fn().mockResolvedValue([]) }))
    await waitFor(() => expect(container.firstChild).toBeNull())
  })

  it('renders a card per invitation once loaded', async () => {
    renderWith(fakeApi())
    expect(await screen.findByText('Chez Bob')).toBeDefined()
  })

  it('accepts an invitation and navigates to the joined space via onAccepted', async () => {
    const { onAccepted } = renderWith(fakeApi())
    fireEvent.click(await screen.findByText('received.action_accept'))
    await waitFor(() => expect(onAccepted).toHaveBeenCalledWith('s-2'))
  })

  it('navigates only after the acceptance and its invalidations have settled, not before', async () => {
    // Regression: navigation used to run from the mutate() call-site's
    // onSuccess, racing the ['spaces'] invalidation it depends on.
    const queryClient = createTestQueryClient()
    let resolveInvalidate!: () => void
    const deferred = new Promise<void>((resolve) => { resolveInvalidate = resolve })
    vi.spyOn(queryClient, 'invalidateQueries').mockReturnValue(deferred)
    const api = fakeApi()
    const { onAccepted } = renderWith(api, vi.fn(), queryClient)

    fireEvent.click(await screen.findByText('received.action_accept'))
    await waitFor(() => expect(api.acceptInvitation).toHaveBeenCalled())
    await new Promise((r) => setTimeout(r, 0))
    expect(onAccepted).not.toHaveBeenCalled()

    resolveInvalidate()
    await waitFor(() => expect(onAccepted).toHaveBeenCalledWith('s-2'))
  })

  it('shows an error and does not call onAccepted when acceptance fails', async () => {
    const { onAccepted } = renderWith(
      fakeApi({ acceptInvitation: vi.fn().mockRejectedValue(new Error('boom')) })
    )
    fireEvent.click(await screen.findByText('received.action_accept'))
    await waitFor(() => expect(screen.getByRole('alert')).toBeDefined())
    expect(onAccepted).not.toHaveBeenCalled()
  })
})
