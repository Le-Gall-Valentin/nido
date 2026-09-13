import type { SpaceMember } from './types'

/**
 * Narrow port for reading a space's member list: the one method a consumer needs when all it wants is
 * an assignee picker (pages/tasks) or a payer list (pages/finance).
 *
 * <p>It was first carved out to keep those pages from importing another page's port, back when the
 * full port lived in pages/spaces. That reason is gone — {@link ISpaceApi} is in this entity now —
 * and it is kept on the other, better one: a consumer of one method should not depend on a port of
 * fifteen, and a test that needs a member list should not have to stub fourteen writes. The same
 * {@code spaceApi} satisfies both shapes, so the router hands one object to both providers.
 */
export interface ISpaceMembersApi {
  listMembers(spaceId: string): Promise<SpaceMember[]>
}
