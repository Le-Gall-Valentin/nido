// @vitest-environment node
import { describe, expect, it } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { InvalidLinkError, NetworkError, RateLimitError, ServerError, WeakPasswordError } from './apiErrors'
import { linkGone, toApiError } from './toApiError'

function axiosError(status?: number, headers: Record<string, string> = {}): AxiosError {
  return new axios.AxiosError('error', undefined, undefined, undefined, status === undefined ? undefined : {
    status, data: {}, headers, config: {} as never, statusText: String(status),
  })
}

describe('toApiError', () => {
  it('lets the request name what its own statuses mean first', () => {
    expect(toApiError(axiosError(410), linkGone)).toBeInstanceOf(InvalidLinkError)
    expect(toApiError(axiosError(400), (s) => (s === 400 ? new WeakPasswordError() : null))).toBeInstanceOf(WeakPasswordError)
  })

  it('reads a refusal to answer more as a rate limit, with its delay', () => {
    const error = toApiError(axiosError(429, { 'retry-after': '42' }))
    expect(error).toBeInstanceOf(RateLimitError)
    expect((error as RateLimitError).retryAfterSeconds).toBe(42)
  })

  it('reads any other answer as a server error, and no answer as no network', () => {
    expect(toApiError(axiosError(500))).toBeInstanceOf(ServerError)
    expect(toApiError(axiosError(410))).toBeInstanceOf(ServerError)
    expect(toApiError(axiosError())).toBeInstanceOf(NetworkError)
    expect(toApiError(new Error('boom'))).toBeInstanceOf(NetworkError)
  })
})
