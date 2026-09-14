import { useQuery } from '@tanstack/react-query'
import { SPACES_QUERY_KEY } from '@/entities/space'
import { useSpacesApi } from './spacesApiContext'


interface UseMySpacesOptions {
  /**
   * Lets a caller that isn't always allowed to fetch (e.g. an unauthenticated
   * visitor on a catch-all route) skip the request instead of guarding the
   * hook call itself, which React's rules of hooks forbid. Defaults to true.
   */
  enabled?: boolean
}

export function useMySpaces(options?: UseMySpacesOptions) {
  const api = useSpacesApi()
  return useQuery({
    queryKey: [SPACES_QUERY_KEY],
    queryFn: () => api.listMySpaces(),
    enabled: options?.enabled ?? true,
  })
}
