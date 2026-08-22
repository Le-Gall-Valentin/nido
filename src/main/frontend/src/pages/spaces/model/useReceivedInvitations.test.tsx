import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { ReceivedInvitation } from '@/entities/space'
import type { ISpacesPageApi } from './ISpacesPageApi'
import { SpacesPageApiProvider } from './spacesPageApiContext'
import { useReceivedInvitations, RECEIVED_INVITATIONS_QUERY_KEY } from './useReceivedInvitations'

const RECEIVED: ReceivedInvitation[] = [
  { invitationId: 'i-1', spaceId: 's-2', spaceName: 'Chez Bob', spaceAccent: '#4a7fa0', spaceGlyph: '🌿', role: 'MEMBER', expiresAt: '2024-01-08T00:00:00Z' },
]

function fakeApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn(), listMembers: vi.fn(), listInvitations: vi.fn(),
    listReceivedInvitations: vi.fn().mockResolvedValue(RECEIVED),
    createSpace: vi.fn(), updateSpace: vi.fn(), deleteSpace: vi.fn(),
    changeMemberRole: vi.fn(), removeMember: vi.fn(), transferOwnership: vi.fn(),
    leaveSpace: vi.fn(), inviteMember: vi.fn(), revokeInvitation: vi.fn(), acceptInvitation: vi.fn(),
    ...overrides,
  }
}

function wrapperFor(api: ISpacesPageApi) {
  const queryClient = createTestQueryClient()
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SpacesPageApiProvider api={api}>{children}</SpacesPageApiProvider>
    </QueryClientProvider>
  )
}

describe('useReceivedInvitations', () => {
  it('exposes the ["invitations", "received"] key', () => {
    expect(RECEIVED_INVITATIONS_QUERY_KEY).toEqual(['invitations', 'received'])
  })

  it('fetches received invitations through the injected api', async () => {
    const api = fakeApi()
    const { result } = renderHook(() => useReceivedInvitations(), { wrapper: wrapperFor(api) })
    await waitFor(() => expect(result.current.data).toEqual(RECEIVED))
    expect(api.listReceivedInvitations).toHaveBeenCalledOnce()
  })
})
