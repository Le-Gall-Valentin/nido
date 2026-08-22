import { useParams } from 'react-router-dom'
import type { SpaceSummary } from '@/entities/space'
import { useMySpaces } from './useMySpaces'

interface ActiveSpace {
  spaceId: string | undefined
  space: SpaceSummary | undefined
  isLoading: boolean
}

/**
 * The active context comes from the URL, never from the store: two tabs open
 * on two contexts must display two contexts. The store only remembers the
 * last choice, to restore navigation on an unscoped entry point.
 */
export function useActiveSpace(): ActiveSpace {
  const { spaceId } = useParams<{ spaceId: string }>()
  const { data: spaces, isLoading } = useMySpaces()
  const space = spaceId ? spaces?.find((s) => s.id === spaceId) : undefined
  return { spaceId, space, isLoading }
}
