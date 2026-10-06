import { NetworkError, RateLimitError, WeakPasswordError } from '@/shared/lib'

export interface FormError {
  key: string
  seconds?: number
}

/** The message a form shows for a failure it does not handle itself. Keys of the passwordReset namespace. */
export function describeError(error: unknown): FormError {
  if (error instanceof WeakPasswordError) return { key: 'error.weak' }
  if (error instanceof RateLimitError) {
    return error.retryAfterSeconds !== null
      ? { key: 'error.rateLimitWithDelay', seconds: error.retryAfterSeconds }
      : { key: 'error.rateLimit' }
  }
  if (error instanceof NetworkError) return { key: 'error.network' }
  return { key: 'error.server' }
}
