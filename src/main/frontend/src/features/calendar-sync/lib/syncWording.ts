import { CalendarSyncError, SYNC_REASONS } from '@/entities/calendar'

type Translate = (key: string, options?: Record<string, unknown>) => string

/** The details the server gives for an event it does not import, each worded under `import.ignored_reason`. */
export const IGNORED_DETAILS = [
  'VTODO', 'VJOURNAL', 'VFREEBUSY', 'NO_START', 'CANCELLED', 'TOO_LONG', 'DUPLICATE_UID', 'TOO_HEAVY', 'BAD_RULE',
  'MALFORMED', 'NIDO_SOURCE',
]

/** The fields whose text can be shortened, each worded under `import.text_field`. */
export const TEXT_FIELDS = ['title', 'description', 'location']

/** Why a followed calendar could not be read, each worded under `feed_error` — HTTP_<status> aside. */
export const FEED_ERRORS = ['INVALID_URL', 'BLOCKED_ADDRESS', 'UNREACHABLE', 'TIMEOUT', 'TOO_MANY_REDIRECTS', 'TOO_LARGE', 'NOT_ICS']

/** Why a followed calendar could not be read: "délai dépassé", "le serveur a répondu 404" — never a raw key. */
export function feedErrorText(t: Translate, code: string): string {
  if (/^HTTP_\d{3}$/.test(code)) return t('feed_error.HTTP', { status: code.slice(5) })
  return t(FEED_ERRORS.includes(code) ? `feed_error.${code}` : 'feed_error.unknown')
}

/** Why an event is not imported — a generic reason for a detail this client does not know, never a raw key. */
export function ignoredReasonText(t: Translate, detail: string): string {
  return IGNORED_DETAILS.includes(detail) ? t(`import.ignored_reason.${detail}`) : t('import.ignored_unknown')
}

/** What a failed sync call says to the reader — a generic message for anything unforeseen. */
export function syncErrorMessage(t: Translate, error: unknown): string {
  if (!(error instanceof CalendarSyncError)) return t('errors.server')
  if (error.reason === 'feed_unavailable') {
    return t('errors.feed_unavailable', { reason: feedErrorText(t, error.feedError ?? 'UNREACHABLE') })
  }
  return t(SYNC_REASONS.includes(error.reason) ? `errors.${error.reason}` : 'errors.server')
}
