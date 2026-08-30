import { useMemo } from 'react'
import { canWrite } from '@/entities/space'
import { useMySpaces } from './useMySpaces'

/**
 * The subset of the caller's own contexts that are valid transfer
 * destinations: not the one they're currently in, and only where they can
 * write — a context where the caller is a VIEWER is never offered, for
 * either copy or move (see the cross-context transfer design doc).
 */
export function useWritableSpaces(excludeSpaceId: string | undefined) {
  const { data: spaces, ...queryRest } = useMySpaces()

  const writable = useMemo(
    () => (spaces ?? []).filter((space) => space.id !== excludeSpaceId && canWrite(space.myRole)),
    [spaces, excludeSpaceId]
  )

  return { ...queryRest, data: writable }
}
