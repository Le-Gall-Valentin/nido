import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { SpaceMember } from '@/entities/space'
import type { ISpacesPageApi } from './ISpacesPageApi'
import { SpacesPageApiProvider } from './spacesPageApiContext'
import { useSpaceMembers, spaceMembersKey } from './useSpaceMembers'

const MEMBERS: SpaceMember[] = [
  { userId: 'u-1', username: 'alice', email: 'alice@test.com', role: 'OWNER', joinedAt: '2024-01-01T00:00:00Z' },
]

function fakeApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn(), listMembers: vi.fn().mockResolvedValue(MEMBERS),
    listInvitations: vi.fn(), listReceivedInvitations: vi.fn(),
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

describe('useSpaceMembers', () => {
  it('builds the ["space", spaceId, "members"] key', () => {
    expect(spaceMembersKey('s-1')).toEqual(['space', 's-1', 'members'])
  })

  it('fetches members through the injected api', async () => {
    const api = fakeApi()
    const { result } = renderHook(() => useSpaceMembers('s-1'), { wrapper: wrapperFor(api) })
    await waitFor(() => expect(result.current.data).toEqual(MEMBERS))
    expect(api.listMembers).toHaveBeenCalledWith('s-1')
  })

  it('does not run when spaceId is absent', () => {
    const api = fakeApi()
    const { result } = renderHook(() => useSpaceMembers(undefined), { wrapper: wrapperFor(api) })
    expect(result.current.fetchStatus).toBe('idle')
    expect(api.listMembers).not.toHaveBeenCalled()
  })
})
