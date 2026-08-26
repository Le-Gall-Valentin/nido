import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SPACES_QUERY_KEY } from '@/features/space-switcher'
import type { ISpacesPageApi } from './ISpacesPageApi'
import { SpacesPageApiProvider } from './spacesPageApiContext'
import { spaceDetailKey } from './useSpaceDetail'
import { spaceMembersKey } from './useSpaceMembers'
import { spaceInvitationsKey } from './useSpaceInvitations'
import { RECEIVED_INVITATIONS_QUERY_KEY } from './useReceivedInvitations'
import {
  useCreateSpace,
  useUpdateSpace,
  useDeleteSpace,
  useChangeMemberRole,
  useRemoveMember,
  useTransferOwnership,
  useLeaveSpace,
  useInviteMember,
  useRevokeInvitation,
  useAcceptInvitation,
} from './useSpaceMutations'

function fakeApi(overrides: Partial<ISpacesPageApi> = {}): ISpacesPageApi {
  return {
    getSpaceDetail: vi.fn(), listMembers: vi.fn(), listInvitations: vi.fn(), listReceivedInvitations: vi.fn(),
    createSpace: vi.fn().mockResolvedValue({}),
    updateSpace: vi.fn().mockResolvedValue(undefined),
    deleteSpace: vi.fn().mockResolvedValue(undefined),
    changeMemberRole: vi.fn().mockResolvedValue(undefined),
    removeMember: vi.fn().mockResolvedValue(undefined),
    transferOwnership: vi.fn().mockResolvedValue(undefined),
    leaveSpace: vi.fn().mockResolvedValue(undefined),
    inviteMember: vi.fn().mockResolvedValue({}),
    revokeInvitation: vi.fn().mockResolvedValue(undefined),
    acceptInvitation: vi.fn().mockResolvedValue({ spaceId: 's-2' }),
    ...overrides,
  }
}

function setup(api: ISpacesPageApi) {
  const queryClient = createTestQueryClient()
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SpacesPageApiProvider api={api}>{children}</SpacesPageApiProvider>
    </QueryClientProvider>
  )
  return { queryClient, wrapper }
}

function spy(queryClient: QueryClient) {
  return vi.spyOn(queryClient, 'invalidateQueries')
}

describe('useCreateSpace', () => {
  it('calls createSpace and invalidates ["spaces"]', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useCreateSpace(), { wrapper })

    result.current.mutate({ name: 'Chez nous', accent: '#c17a5c', glyph: '🏡' })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.createSpace).toHaveBeenCalledWith({ name: 'Chez nous', accent: '#c17a5c', glyph: '🏡' })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: [SPACES_QUERY_KEY] })
  })
})

describe('useUpdateSpace', () => {
  it('invalidates the detail and ["spaces"]', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useUpdateSpace('s-1'), { wrapper })

    result.current.mutate({ name: 'New name' })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.updateSpace).toHaveBeenCalledWith('s-1', { name: 'New name' })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: spaceDetailKey('s-1') })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: [SPACES_QUERY_KEY] })
  })

  it('does not resolve until both invalidations have settled', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    let resolveInvalidate!: () => void
    const deferred = new Promise<void>((resolve) => { resolveInvalidate = resolve })
    vi.spyOn(queryClient, 'invalidateQueries').mockReturnValue(deferred)
    const { result } = renderHook(() => useUpdateSpace('s-1'), { wrapper })

    result.current.mutate({ name: 'New name' })
    await waitFor(() => expect(api.updateSpace).toHaveBeenCalled())
    await new Promise((r) => setTimeout(r, 0))
    expect(result.current.isSuccess).toBe(false)

    resolveInvalidate()
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
  })
})

describe('useDeleteSpace', () => {
  it('invalidates ["spaces"] so the route guard notices the space is gone', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useDeleteSpace('s-1'), { wrapper })

    result.current.mutate()
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.deleteSpace).toHaveBeenCalledWith('s-1')
    expect(invalidate).toHaveBeenCalledWith({ queryKey: [SPACES_QUERY_KEY] })
  })
})

describe('useChangeMemberRole', () => {
  it('invalidates only the members list', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useChangeMemberRole('s-1'), { wrapper })

    result.current.mutate({ userId: 'u-2', role: 'ADMIN' })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.changeMemberRole).toHaveBeenCalledWith('s-1', 'u-2', 'ADMIN')
    expect(invalidate).toHaveBeenCalledExactlyOnceWith({ queryKey: spaceMembersKey('s-1') })
  })
})

describe('useRemoveMember', () => {
  it('invalidates only the detail (which covers the members list by key prefix) and ["spaces"]', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useRemoveMember('s-1'), { wrapper })

    result.current.mutate('u-2')
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.removeMember).toHaveBeenCalledWith('s-1', 'u-2')
    expect(invalidate).toHaveBeenCalledTimes(2)
    expect(invalidate).toHaveBeenCalledWith({ queryKey: spaceDetailKey('s-1') })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: [SPACES_QUERY_KEY] })
  })

  it('does not resolve until both invalidations have settled', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    let resolveInvalidate!: () => void
    const deferred = new Promise<void>((resolve) => { resolveInvalidate = resolve })
    vi.spyOn(queryClient, 'invalidateQueries').mockReturnValue(deferred)
    const { result } = renderHook(() => useRemoveMember('s-1'), { wrapper })

    result.current.mutate('u-2')
    await waitFor(() => expect(api.removeMember).toHaveBeenCalled())
    await new Promise((r) => setTimeout(r, 0))
    expect(result.current.isSuccess).toBe(false)

    resolveInvalidate()
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
  })
})

describe('useTransferOwnership', () => {
  it('invalidates only the detail (which covers the members list by key prefix) and ["spaces"]', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useTransferOwnership('s-1'), { wrapper })

    result.current.mutate('u-2')
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.transferOwnership).toHaveBeenCalledWith('s-1', 'u-2')
    expect(invalidate).toHaveBeenCalledTimes(2)
    expect(invalidate).toHaveBeenCalledWith({ queryKey: spaceDetailKey('s-1') })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: [SPACES_QUERY_KEY] })
  })

  it('does not resolve until both invalidations have settled', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    let resolveInvalidate!: () => void
    const deferred = new Promise<void>((resolve) => { resolveInvalidate = resolve })
    vi.spyOn(queryClient, 'invalidateQueries').mockReturnValue(deferred)
    const { result } = renderHook(() => useTransferOwnership('s-1'), { wrapper })

    result.current.mutate('u-2')
    await waitFor(() => expect(api.transferOwnership).toHaveBeenCalled())
    await new Promise((r) => setTimeout(r, 0))
    expect(result.current.isSuccess).toBe(false)

    resolveInvalidate()
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
  })
})

describe('useLeaveSpace', () => {
  it('invalidates only ["spaces"]; redirecting away is the component\'s job', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useLeaveSpace('s-1'), { wrapper })

    result.current.mutate()
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.leaveSpace).toHaveBeenCalledWith('s-1')
    expect(invalidate).toHaveBeenCalledExactlyOnceWith({ queryKey: [SPACES_QUERY_KEY] })
  })
})

describe('useInviteMember', () => {
  it('invalidates only the issued invitations list', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useInviteMember('s-1'), { wrapper })

    result.current.mutate({ email: 'carol@test.com', role: 'MEMBER' })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.inviteMember).toHaveBeenCalledWith('s-1', 'carol@test.com', 'MEMBER')
    expect(invalidate).toHaveBeenCalledExactlyOnceWith({ queryKey: spaceInvitationsKey('s-1') })
  })
})

describe('useRevokeInvitation', () => {
  it('invalidates only the issued invitations list', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useRevokeInvitation('s-1'), { wrapper })

    result.current.mutate('i-1')
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.revokeInvitation).toHaveBeenCalledWith('s-1', 'i-1')
    expect(invalidate).toHaveBeenCalledExactlyOnceWith({ queryKey: spaceInvitationsKey('s-1') })
  })
})

describe('useAcceptInvitation', () => {
  it('invalidates ["spaces"] and ["invitations", "received"]', async () => {
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    const invalidate = spy(queryClient)
    const { result } = renderHook(() => useAcceptInvitation(), { wrapper })

    result.current.mutate('i-1')
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(api.acceptInvitation).toHaveBeenCalledWith('i-1')
    expect(result.current.data).toEqual({ spaceId: 's-2' })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: [SPACES_QUERY_KEY] })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: RECEIVED_INVITATIONS_QUERY_KEY })
  })

  it('does not resolve until the invalidations it triggers have settled', async () => {
    // A caller awaiting this mutation (e.g. to navigate afterwards) must see
    // the refetch complete first, or it acts on stale cached data.
    const api = fakeApi()
    const { queryClient, wrapper } = setup(api)
    let resolveInvalidate!: () => void
    const deferred = new Promise<void>((resolve) => { resolveInvalidate = resolve })
    vi.spyOn(queryClient, 'invalidateQueries').mockReturnValue(deferred)
    const { result } = renderHook(() => useAcceptInvitation(), { wrapper })

    result.current.mutate('i-1')
    await waitFor(() => expect(api.acceptInvitation).toHaveBeenCalled())
    await new Promise((r) => setTimeout(r, 0))
    expect(result.current.isSuccess).toBe(false)

    resolveInvalidate()
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
  })
})
