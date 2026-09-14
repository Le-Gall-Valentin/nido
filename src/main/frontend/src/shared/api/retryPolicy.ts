import { isAxiosError } from 'axios'

const MAX_RETRIES = 1

/**
 * Statuses a second attempt cannot change, so asking again only costs.
 *
 * <p>401 is the one that mattered: the refresh interceptor already handled it — refreshed the
 * session, replayed the request — and a react-query retry starts that whole sequence over. Measured
 * before this existed: one query with a dead session cost <b>two</b> refresh calls, and the seven the
 * finance page opens with cost <b>eight</b>, against a server limit of five per minute. That limit is
 * keyed on the IP when nobody is authenticated, which is exactly the case here, so a single page load
 * left the whole household unable to refresh for a minute — and the next legitimate expiry inside that
 * minute reads as a dead session and signs them out.
 *
 * <p>403, 404 and 422 are refusals about what was asked, not about whether it arrived; 429 is the
 * limiter, and retrying into it is what fills it.
 */
const NOT_WORTH_ASKING_TWICE = new Set([401, 403, 404, 422, 429])

/**
 * Whether react-query should try a failed query again — the {@code retry} option in App.tsx.
 *
 * <p>Keeps the one retry that {@code retry: 1} was for (the server broke, the network dropped: a
 * second attempt can genuinely succeed) and drops it where the answer is already known.
 */
export function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  if (failureCount >= MAX_RETRIES) return false
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status !== undefined && NOT_WORTH_ASKING_TWICE.has(status)) return false
  }
  return true
}
