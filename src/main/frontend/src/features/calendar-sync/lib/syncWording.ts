import { CalendarSyncError, SYNC_REASONS } from '@/entities/calendar'

type Translate = (key: string, options?: Record<string, unknown>) => string

/** The details the server gives for an event it does not import, each worded under `import.ignored_reason`. */
const IGNORED_DETAILS = [
  'VTODO', 'VJOURNAL', 'VFREEBUSY', 'NO_START', 'CANCELLED', 'TOO_LONG', 'DUPLICATE_UID', 'TOO_HEAVY', 'BAD_RULE',
  'NIDO_SOURCE',
]

/** Why a followed calendar could not be read: "délai dépassé", "le serveur a répondu 404". */
export function feedErrorText(t: Translate, code: string): string {
  return code.startsWith('HTTP_') ? t('feed_error.HTTP', { status: code.slice(5) }) : t(`feed_error.${code}`)
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
