import { useEffect, type CSSProperties } from 'react'
import { Outlet } from 'react-router-dom'
import { safeAccent } from '@/entities/space'
import { activeSpaceStore, useActiveSpace } from '@/features/space-switcher'

/**
 * Root of a scoped /s/:spaceId subtree: remembers the URL's context as the
 * last choice (so an unscoped visit can restore it later) and exposes it as
 * an accent CSS variable for anything under the outlet to pick up. The
 * accent is only ever read through safeAccent, never straight from the API.
 */
export function SpaceLayout() {
  const { spaceId, space } = useActiveSpace()

  useEffect(() => {
    if (spaceId) activeSpaceStore.getState().remember(spaceId)
  }, [spaceId])

  return (
    <div className="contents" data-testid="space-layout" style={{ '--space-accent': safeAccent(space?.accent) } as CSSProperties}>
      <Outlet />
    </div>
  )
}
