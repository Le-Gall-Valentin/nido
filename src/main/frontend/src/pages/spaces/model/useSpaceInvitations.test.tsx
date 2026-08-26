import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { SpaceInvitation } from '@/entities/space'
import type { ISpacesPageApi } from './ISpacesPageApi'
import { SpacesPageApiProvider } from './spacesPageApiContext'
import { useSpaceInvitations, spaceInvitationsKey } from './useSpaceInvitations'

const INVITATIONS: SpaceInvitation[] = [
  { id: 'i-1', email: 'carol@test.com', role: 'MEMBER', code: 'NIDO-ABC123', status: 'PENDING', expiresAt: '2024-01-08T00:00:00Z', createdAt: '2024-01-01T00:00:00Z' },
]

function fakeApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn(), listMembers: vi.fn(),
    listInvitations: vi.fn().mockResolvedValue(INVITATIONS), listReceivedInvitations: vi.fn(),
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

describe('useSpaceInvitations', () => {
  it('builds the ["space", spaceId, "invitations"] key', () => {
    expect(spaceInvitationsKey('s-1')).toEqual(['space', 's-1', 'invitations'])
  })

  it('fetches invitations through the injected api', async () => {
    const api = fakeApi()
    const { result } = renderHook(() => useSpaceInvitations('s-1'), { wrapper: wrapperFor(api) })
    await waitFor(() => expect(result.current.data).toEqual(INVITATIONS))
    expect(api.listInvitations).toHaveBeenCalledWith('s-1')
  })

  it('does not run when spaceId is absent', () => {
    const api = fakeApi()
    const { result } = renderHook(() => useSpaceInvitations(undefined), { wrapper: wrapperFor(api) })
    expect(result.current.fetchStatus).toBe('idle')
    expect(api.listInvitations).not.toHaveBeenCalled()
  })

  it('does not run when explicitly disabled, e.g. for a role that cannot manage the space', () => {
    const api = fakeApi()
    const { result } = renderHook(() => useSpaceInvitations('s-1', false), { wrapper: wrapperFor(api) })
    expect(result.current.fetchStatus).toBe('idle')
    expect(api.listInvitations).not.toHaveBeenCalled()
  })
})
