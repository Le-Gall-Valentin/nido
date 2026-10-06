import { isAxiosError } from 'axios'
import { InvalidLinkError, NetworkError, RateLimitError, ServerError } from './apiErrors'
import { parseRetryAfter } from './parseRetryAfter'

/** What a request failed with: `specific` first, then a rate limit, a server error or no network at all. */
export function toApiError(error: unknown, specific: (status: number) => Error | null = () => null): Error {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status !== undefined) {
      const known = specific(status)
      if (known) return known
      if (status === 429) return new RateLimitError(parseRetryAfter(error.response?.headers))
      return new ServerError()
    }
  }
  return new NetworkError()
}

/** 410: a one-time link — reset or invitation — no longer works. */
export const linkGone = (status: number): Error | null => (status === 410 ? new InvalidLinkError() : null)
