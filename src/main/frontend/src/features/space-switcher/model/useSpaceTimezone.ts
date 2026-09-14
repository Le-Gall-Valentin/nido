import type { SpaceSummary } from '@/entities/space'
import { useMySpaces } from './useMySpaces'

/**
 * Lives beside useMySpaces rather than in entities/space: it reads that hook, and an entity may not
 * depend on a feature — eslint-plugin-boundaries fails the build over it, which is how this landed
 * here rather than one layer down.
 *
 * The calendar a space keeps, or `undefined` while the list is still loading or if the id names no
 * space the viewer belongs to.
 *
 * Undefined on purpose rather than a guess: passed on to `todayIso`, it reads the viewer's own
 * calendar, which is the best available answer for the moment before the list arrives — and never a
 * date computed in a zone nobody chose.
 *
 * Pure, so it can be tested without a query client; the hook below is the thin part.
 */
export function spaceTimezoneOf(
  spaces: Pick<SpaceSummary, 'id' | 'timezone'>[] | undefined,
  spaceId: string
): string | undefined {
  return spaces?.find((space) => space.id === spaceId)?.timezone
}

/**
 * Reads it from the spaces the viewer already has loaded — every screen that shows a space has
 * asked for that list anyway, so this costs no extra request.
 */
export function useSpaceTimezone(spaceId: string): string | undefined {
  const { data: spaces } = useMySpaces()
  return spaceTimezoneOf(spaces, spaceId)
}
