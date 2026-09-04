import type { SpaceMember } from './types'

/**
 * Narrow port for reading a space's member list — the subset of the
 * "Membres et groupes" page's own ISpacesPageApi that other pages (like
 * pages/tasks, for its assignee picker) are allowed to depend on without
 * importing from another page. The real implementation (spacesPageApi)
 * already satisfies this shape as-is; no new axios client is needed.
 */
export interface ISpaceMembersApi {
  listMembers(spaceId: string): Promise<SpaceMember[]>
}
