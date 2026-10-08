import { NetworkError, RateLimitError } from '@/shared/lib'
import { SendLimitError } from './errors'

/** A message to show: an i18n key and its values. */
export interface Message {
  key: string
  values?: Record<string, unknown>
}

export const minutesOf = (seconds: number) => Math.ceil(seconds / 60)

/**
 * The words for what any screen that sends or checks a code can be told, whatever else it says: each screen words
 * what is its own and leaves the rest here. The keys name their namespace, so a screen of any namespace shows them.
 */
export function commonErrorMessage(error: unknown): Message {
  if (error instanceof SendLimitError) return { key: 'twoFactor:error.send_limit', values: { minutes: minutesOf(error.seconds) } }
  if (error instanceof RateLimitError) {
    return error.retryAfterSeconds !== null
      ? { key: 'twoFactor:error.rate_limit_timed', values: { seconds: error.retryAfterSeconds } }
      : { key: 'twoFactor:error.rate_limit' }
  }
  if (error instanceof NetworkError) return { key: 'twoFactor:error.network' }
  return { key: 'twoFactor:error.server' }
}
