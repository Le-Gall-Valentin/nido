/**
 * The list of contexts the caller belongs to is a user-global resource: it does not depend on which
 * context is currently active, unlike every other space-scoped query in this app (keyed
 * ['space', spaceId, ...]). It is therefore the one documented exception to the context-prefix
 * convention — do not imitate this key shape for anything that actually varies per space.
 *
 * <p>It lives here, apart from the query that reads it, because the writes that invalidate it are in
 * this entity while the query itself belongs to the switcher: the key is the contract between them,
 * and an entity cannot import a feature.
 */
export const SPACES_QUERY_KEY = 'spaces' as const
