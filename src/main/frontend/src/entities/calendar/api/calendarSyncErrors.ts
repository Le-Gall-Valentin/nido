import { isAxiosError } from 'axios'

/** Every reason a sync call can fail for, so that a dialog's wording of them is checked to be complete. */
export const SYNC_REASONS = [
  'invalid_sources', 'not_ics', 'import_too_large', 'file_too_large', 'upload_too_large', 'too_many_links', 'public_url_missing',
  'too_many_feeds', 'refresh_too_soon', 'invalid_feed_address', 'feed_unavailable',
  'rate_limited', 'forbidden', 'server', 'network',
] as const

export type SyncReason = (typeof SYNC_REASONS)[number]

/**
 * Why a sync call failed, as the server named it (`error_code`) or as the call ended: what the dialog words for the
 * reader. `feedError` says why a calendar could not be followed — "TIMEOUT", "HTTP_404".
 */
export class CalendarSyncError extends Error {
  constructor(readonly reason: SyncReason, readonly feedError?: string) {
    super(reason)
    this.name = 'CalendarSyncError'
  }
}

export function toSyncError(error: unknown): CalendarSyncError {
  if (isAxiosError(error) && error.response) {
    const { status } = error.response
    const data = (error.response.data ?? {}) as { error_code?: string; feed_error?: string }
    if (typeof data.error_code === 'string') {
      // A code this client does not know is still a server refusal, not a reason to word.
      const known = (SYNC_REASONS as readonly string[]).includes(data.error_code)
      return new CalendarSyncError(known ? data.error_code as SyncReason : 'server', data.feed_error)
    }
    // A 413 without nido's code was answered before nido: a reverse proxy's own limit, lower than nido's 5 MB.
    if (status === 413) return new CalendarSyncError('upload_too_large')
    if (status === 429) return new CalendarSyncError('rate_limited')
    if (status === 403) return new CalendarSyncError('forbidden')
    return new CalendarSyncError('server')
  }
  return new CalendarSyncError('network')
}
