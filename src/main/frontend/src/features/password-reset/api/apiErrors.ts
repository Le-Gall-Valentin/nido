import { isAxiosError } from 'axios'
import { NetworkError, parseRetryAfter, RateLimitError, ServerError } from '@/shared/lib'
import { InvalidResetLinkError } from '../model/errors'

/** What a request of this feature failed with: `specific` first, then a rate limit, a server error or no network. */
export function toError(error: unknown, specific: (status: number) => Error | null = () => null): Error {
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

/** 410: the link expired, was used, was replaced or never existed — a reset link and an invitation link alike. */
export const linkNoLongerWorks = (status: number) => (status === 410 ? new InvalidResetLinkError() : null)
