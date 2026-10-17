import { isAxiosError } from 'axios'

/**
 * Why a sync call failed, as the server named it (`error_code`) or as the call ended: what the dialog words for the
 * reader. `feedError` says why a calendar could not be followed — "TIMEOUT", "HTTP_404".
 */
export class CalendarSyncError extends Error {
  constructor(readonly reason: string, readonly feedError?: string) {
    super(reason)
    this.name = 'CalendarSyncError'
  }
}

export function toSyncError(error: unknown): CalendarSyncError {
  if (isAxiosError(error) && error.response) {
    const { status } = error.response
    const data = (error.response.data ?? {}) as { error_code?: string; feed_error?: string }
    if (typeof data.error_code === 'string') return new CalendarSyncError(data.error_code, data.feed_error)
    if (status === 413) return new CalendarSyncError('file_too_large')
    if (status === 429) return new CalendarSyncError('rate_limited')
    if (status === 403) return new CalendarSyncError('forbidden')
    return new CalendarSyncError('server')
  }
  return new CalendarSyncError('network')
}
