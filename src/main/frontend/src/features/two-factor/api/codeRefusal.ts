import type { AxiosResponse } from 'axios'
import { parseRetryAfter } from '@/shared/lib'
import { ResendTooSoonError, SendLimitError } from '../model/errors'

/**
 * A code that could not leave, as every route that sends one says it: `resend_too_soon` or `send_limit_reached`,
 * with the wait in `retryAfterSeconds` (or `Retry-After`). Anything else is not a refusal of a code: null.
 */
export function codeRefusalOf(response: Pick<AxiosResponse, 'data' | 'headers'>): ResendTooSoonError | SendLimitError | null {
  const data = (response.data ?? {}) as { error_code?: string; retryAfterSeconds?: number }
  const wait = data.retryAfterSeconds ?? parseRetryAfter(response.headers)
  if (data.error_code === 'resend_too_soon') return new ResendTooSoonError(wait ?? 60)
  if (data.error_code === 'send_limit_reached') return new SendLimitError(wait ?? 900)
  return null
}
