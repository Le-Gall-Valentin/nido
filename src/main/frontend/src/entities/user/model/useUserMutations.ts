import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { UsersPage, AdminUser } from '../api/adminUsersApi'
import { useAdminUsersApi } from './adminUsersApiContext'
import { USERS_QUERY_KEY } from './useUsers'

function useInvalidateUsers() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] })
}

export function useCreateUser() {
  const api = useAdminUsersApi()
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ username, email, password, role }: {
      username: string
      email: string
      password: string
      role: 'USER' | 'ADMIN'
    }) => api.createUser(username, email, password, role),
    onSuccess: invalidate,
  })
}

export function useUpdateUserRole() {
  const api = useAdminUsersApi()
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: 'USER' | 'ADMIN' }) =>
      api.updateUserRole(id, role),
    onSuccess: invalidate,
  })
}

export function useDeleteUser() {
  const api = useAdminUsersApi()
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: (id: string) => api.deleteUser(id),
    onSuccess: invalidate,
  })
}

export function useResetTotp() {
  const api = useAdminUsersApi()
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: (id: string) => api.resetTotp(id),
    onSuccess: invalidate,
  })
}

export function useToggleUserActive(page: number, search = '') {
  const api = useAdminUsersApi()
  const queryClient = useQueryClient()
  const queryKey = [USERS_QUERY_KEY, page, search.trim()]
  return useMutation({
    mutationFn: (user: AdminUser) =>
      user.isActive
        ? api.deactivateUser(user.id)
        : api.activateUser(user.id),
    onMutate: async (user: AdminUser) => {
      await queryClient.cancelQueries({ queryKey })
      const previous = queryClient.getQueryData<UsersPage>(queryKey)
      queryClient.setQueryData<UsersPage>(queryKey, old => {
        if (!old) return old
        return {
          ...old,
          content: old.content.map(u =>
            u.id === user.id ? { ...u, isActive: !u.isActive } : u
          ),
        }
      })
      return { previous }
    },
    onError: (_err, _user, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKey, context.previous)
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey }),
  })
}
