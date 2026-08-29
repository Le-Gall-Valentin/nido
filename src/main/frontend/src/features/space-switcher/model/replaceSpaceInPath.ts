/**
 * Swaps the space id in a `/s/:spaceId/...` path for another one, keeping
 * the rest of the path untouched — so switching context refreshes the
 * current page in the new space instead of redirecting to its default tab.
 * Returns the path unchanged when it isn't space-scoped (e.g. `/account`).
 */
export function replaceSpaceInPath(pathname: string, newSpaceId: string): string {
  return pathname.replace(/^\/s\/[^/]+/, `/s/${newSpaceId}`)
}
