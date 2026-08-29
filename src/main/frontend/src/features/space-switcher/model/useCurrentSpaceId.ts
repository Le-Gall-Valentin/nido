import { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { isPersonal } from '@/entities/space'
import { activeSpaceStore } from './activeSpaceStore'
import { useMySpaces } from './useMySpaces'

export interface CurrentSpace {
  spaceId: string | undefined
  isLoading: boolean
}

interface UseCurrentSpaceIdOptions {
  /** Lets a caller that isn't always allowed to fetch skip the request. Defaults to true. */
  enabled?: boolean
}

/**
 * The space every persistent-shell nav item (and the unscoped landing
 * redirect) resolves against: the URL's own spaceId when present — so two
 * tabs on two different contexts stay independent — else the remembered
 * last space (validated against the caller's current list, since it may
 * have disappeared), else the personal space every account has. Unlike
 * useActiveSpace, this is never undefined once the space list has loaded,
 * which is exactly why nav items and DefaultRedirect need it: they must
 * resolve to *some* space even when the current route carries none.
 */
export function useCurrentSpaceId(options?: UseCurrentSpaceIdOptions): CurrentSpace {
  const { spaceId: urlSpaceId } = useParams<{ spaceId: string }>()
  const { data: spaces, isLoading } = useMySpaces({ enabled: options?.enabled ?? true })
  const lastSpaceId = activeSpaceStore((s) => s.lastSpaceId)

  const remembered = lastSpaceId ? spaces?.find((space) => space.id === lastSpaceId) : undefined
  const stale = !!lastSpaceId && !!spaces && !remembered

  useEffect(() => {
    if (stale) activeSpaceStore.getState().forget()
  }, [stale])

  if (urlSpaceId) return { spaceId: urlSpaceId, isLoading: false }
  if (isLoading) return { spaceId: undefined, isLoading: true }
  if (remembered) return { spaceId: remembered.id, isLoading: false }

  const personal = spaces?.find((space) => isPersonal(space))
  return { spaceId: personal?.id, isLoading: false }
}
