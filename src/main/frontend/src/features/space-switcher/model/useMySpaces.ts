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

export function useMySpaces() {
  const api = useSpacesApi()
  return useQuery({
    queryKey: [SPACES_QUERY_KEY],
    queryFn: () => api.listMySpaces(),
  })
}
