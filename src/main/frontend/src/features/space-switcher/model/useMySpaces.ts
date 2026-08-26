import { useQuery } from '@tanstack/react-query'
import { useSpacesApi } from './spacesApiContext'

/**
 * The list of contexts the caller belongs to is a user-global resource: it
 * does not depend on which context is currently active, unlike every other
 * space-scoped query in this app (keyed ['space', spaceId, ...]). It is
 * therefore the one documented exception to the context-prefix convention —
 * do not imitate this key shape for anything that actually varies per space.
 */
export const SPACES_QUERY_KEY = 'spaces' as const

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
