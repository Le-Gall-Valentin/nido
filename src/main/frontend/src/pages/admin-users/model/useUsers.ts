import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useAdminUsersApi } from './adminUsersApiContext'

export const USERS_QUERY_KEY = 'users' as const
export const USERS_PAGE_SIZE = 20

export function useUsers(page: number, search = '') {
  const api = useAdminUsersApi()
  const trimmed = search.trim()
  return useQuery({
    queryKey: [USERS_QUERY_KEY, page, trimmed],
    queryFn: () => api.listUsers(page, USERS_PAGE_SIZE, trimmed || undefined),
    placeholderData: keepPreviousData,
  })
}
