import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { SpaceDetail } from '@/entities/space'
import type { ISpacesPageApi } from './ISpacesPageApi'
import { SpacesPageApiProvider } from './spacesPageApiContext'
import { useSpaceDetail, spaceDetailKey } from './useSpaceDetail'

const DETAIL: SpaceDetail = {
  id: 's-1', type: 'SHARED', name: 'Chez nous', description: null,
  accent: '#c17a5c', glyph: '🏡', myRole: 'OWNER', memberCount: 1,
}

function fakeApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn().mockResolvedValue(DETAIL),
    listMembers: vi.fn(), listInvitations: vi.fn(), listReceivedInvitations: vi.fn(),
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

describe('useSpaceDetail', () => {
  it('builds the ["space", spaceId] key', () => {
    expect(spaceDetailKey('s-1')).toEqual(['space', 's-1'])
  })

  it('fetches the detail through the injected api', async () => {
    const api = fakeApi()
    const { result } = renderHook(() => useSpaceDetail('s-1'), { wrapper: wrapperFor(api) })
    await waitFor(() => expect(result.current.data).toEqual(DETAIL))
    expect(api.getSpaceDetail).toHaveBeenCalledWith('s-1')
  })

  it('does not run when spaceId is absent', () => {
    const api = fakeApi()
    const { result } = renderHook(() => useSpaceDetail(undefined), { wrapper: wrapperFor(api) })
    expect(result.current.fetchStatus).toBe('idle')
    expect(api.getSpaceDetail).not.toHaveBeenCalled()
  })
})
