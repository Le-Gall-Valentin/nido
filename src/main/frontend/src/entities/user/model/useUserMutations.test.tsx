import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { AdminUser } from '@/entities/user'
import type { IAdminUsersApi, UsersPage } from './IAdminUsersApi'
import { AdminUsersApiProvider } from './adminUsersApiContext'
import { USERS_QUERY_KEY } from './useUsers'
import { useToggleUserActive } from './useUserMutations'

const USER: AdminUser = {
  id: 'u-1', username: 'alice', email: 'alice@test.com', role: 'USER', createdAt: '2026-01-01', totpEnabled: false, isActive: true,
}

const PAGE: UsersPage = { content: [USER], totalElements: 1, page: 0, size: 20 }

function fakeApi(overrides: Partial<IAdminUsersApi> = {}): IAdminUsersApi {
  return {
    listUsers: vi.fn(),
    createUser: vi.fn(),
    updateUserRole: vi.fn(),
    activateUser: vi.fn(),
    deactivateUser: vi.fn(),
    resetTotp: vi.fn(),
    deleteUser: vi.fn(),
    ...overrides,
  }
}

describe('useToggleUserActive', () => {
  it('rolls back the optimistic update when the mutation fails', async () => {
    let rejectDeactivate!: (error: Error) => void
    const pending = new Promise<void>((_resolve, reject) => { rejectDeactivate = reject })
    const api = fakeApi({ deactivateUser: vi.fn().mockReturnValue(pending) })
    // Not createTestQueryClient: its gcTime: 0 garbage-collects this entry the
    // instant it has no active useQuery observer, before the rollback can be observed.
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: 0 }, mutations: { retry: false } },
    })
    const queryKey = [USERS_QUERY_KEY, 0, '']
    queryClient.setQueryData<UsersPage>(queryKey, PAGE)
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>
        <AdminUsersApiProvider api={api}>{children}</AdminUsersApiProvider>
      </QueryClientProvider>
    )
    const { result } = renderHook(() => useToggleUserActive(0), { wrapper })

    result.current.mutate(USER)
    await waitFor(() => expect(queryClient.getQueryData<UsersPage>(queryKey)?.content[0].isActive).toBe(false))

    rejectDeactivate(new Error('boom'))
    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(queryClient.getQueryData<UsersPage>(queryKey)?.content[0].isActive).toBe(true)
  })
})
