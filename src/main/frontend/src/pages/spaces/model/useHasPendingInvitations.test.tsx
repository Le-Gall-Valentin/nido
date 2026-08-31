import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { ReceivedInvitation } from '@/entities/space'
import type { ISpacesPageApi } from './ISpacesPageApi'
import { SpacesPageApiProvider } from './spacesPageApiContext'
import { useHasPendingInvitations } from './useHasPendingInvitations'

const RECEIVED: ReceivedInvitation[] = [
  { invitationId: 'i-1', spaceId: 's-2', spaceName: 'Chez Bob', spaceAccent: '#4a7fa0', spaceGlyph: '🌿', role: 'MEMBER', expiresAt: '2024-01-08T00:00:00Z' },
]

function fakeApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn(), listMembers: vi.fn(), listInvitations: vi.fn(),
    listReceivedInvitations: vi.fn().mockResolvedValue([]),
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

describe('useHasPendingInvitations', () => {
  it('is false when there are no received invitations', async () => {
    const api = fakeApi({ listReceivedInvitations: vi.fn().mockResolvedValue([]) })
    const { result } = renderHook(() => useHasPendingInvitations(), { wrapper: wrapperFor(api) })
    await waitFor(() => expect(api.listReceivedInvitations).toHaveBeenCalledOnce())
    expect(result.current).toBe(false)
  })

  it('is true when there is at least one received invitation', async () => {
    const api = fakeApi({ listReceivedInvitations: vi.fn().mockResolvedValue(RECEIVED) })
    const { result } = renderHook(() => useHasPendingInvitations(), { wrapper: wrapperFor(api) })
    await waitFor(() => expect(result.current).toBe(true))
  })
})
