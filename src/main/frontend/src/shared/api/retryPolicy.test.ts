// @vitest-environment node
import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { shouldRetryQuery } from './retryPolicy'

function axiosErrorWith(status: number, url = '/spaces', method = 'get'): AxiosError {
  const config = { url, method, headers: {} as never } as InternalAxiosRequestConfig
  return new AxiosError('failed', String(status), config, null, {
    status, data: null, headers: {} as never, config, statusText: 'failed',
  })
}

describe('shouldRetryQuery', () => {
  it('does not retry an unauthorized response, whatever the count', () => {
    // The interceptor already refreshed and replayed this one. Retrying asks the whole question
    // again, including a second refresh, and the answer is already known.
    expect(shouldRetryQuery(0, axiosErrorWith(401))).toBe(false)
  })

  it('does not retry a refusal either — a role does not change between two attempts', () => {
    expect(shouldRetryQuery(0, axiosErrorWith(403))).toBe(false)
    expect(shouldRetryQuery(0, axiosErrorWith(404))).toBe(false)
    expect(shouldRetryQuery(0, axiosErrorWith(422))).toBe(false)
  })

  it('does not retry a rate limit, which retrying is what caused', () => {
    expect(shouldRetryQuery(0, axiosErrorWith(429))).toBe(false)
  })

  it('retries once when the server broke or the network dropped', () => {
    // What retry: 1 was for, and the only thing a second attempt can fix.
    expect(shouldRetryQuery(0, axiosErrorWith(503))).toBe(true)
    expect(shouldRetryQuery(0, new AxiosError('Network Error', 'ERR_NETWORK'))).toBe(true)
  })

  it('gives up after one retry', () => {
    expect(shouldRetryQuery(1, axiosErrorWith(503))).toBe(false)
  })

  it('retries a failure that is not an axios error at all', () => {
    // A queryFn can throw for its own reasons; nothing here says it is not worth a second go.
    expect(shouldRetryQuery(0, new Error('boom'))).toBe(true)
    expect(shouldRetryQuery(1, new Error('boom'))).toBe(false)
  })
})

describe('the whole path, with the real interceptor', () => {
  it('costs one refresh for an expired session, not two', async () => {
    // Measured before this policy existed: one query cost two refreshes, and seven — what the
    // finance page opens with — cost eight, against a server limit of five per minute keyed on the
    // IP when nobody is authenticated. The household then spends a minute unable to refresh, and
    // the next legitimate expiry inside that minute reads as a dead session.
    const { QueryClient } = await import('@tanstack/react-query')
    const { createRefreshInterceptorHandlers } = await import('./refreshInterceptor')

    const instance = axios.create()
    let refreshCalls = 0
    instance.defaults.adapter = (config) => {
      if (config.url === '/auth/refresh') refreshCalls++
      // The url has to be the one that was asked for: the interceptor reads it to tell a failed
      // refresh from a request worth refreshing for, and a fixture that lies about it loops forever.
      return Promise.reject(axiosErrorWith(401, config.url, config.method))
    }
    const { onFulfilled, onRejected } = createRefreshInterceptorHandlers(instance, () => {})
    instance.interceptors.response.use(onFulfilled, onRejected)

    const client = new QueryClient({ defaultOptions: { queries: { retry: shouldRetryQuery, retryDelay: 0 } } })
    await Promise.allSettled([1, 2, 3, 4, 5, 6, 7].map((n) =>
      client.fetchQuery({ queryKey: ['q' + String(n)], queryFn: () => instance.get('/data' + String(n)) })))

    expect(refreshCalls).toBe(1)
  })
})
