// @vitest-environment node
// Node environment required: tests axios behavior (no DOM needed),
// and the jsdom env interferes with axios adapter selection.
import axios, {AxiosError, type InternalAxiosRequestConfig} from 'axios'
import {beforeEach, describe, expect, it, vi, type Mock} from 'vitest'
import {createRefreshInterceptorHandlers} from './refreshInterceptor'

let mockOnSessionExpired: Mock<() => void>

function make401(url: string, retry = false): AxiosError {
  const config = {
    url,
    headers: axios.defaults.headers as never,
    _retry: retry,
  } as InternalAxiosRequestConfig & { _retry: boolean }
  return new AxiosError('Unauthorized', '401', config, null, {
    status: 401,
    data: null,
    headers: {} as never,
    config,
    statusText: 'Unauthorized',
  })
}

describe('refreshInterceptor', () => {
  beforeEach(() => {
    mockOnSessionExpired = vi.fn()
  })

  it('rejects immediately when error.config is undefined', async () => {
    const instance = axios.create()
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)
    const error = new AxiosError('Network Error', 'ERR_NETWORK', undefined, null, undefined)

    await expect(onRejected(error)).rejects.toBe(error)
    expect(mockOnSessionExpired).not.toHaveBeenCalled()
  })

  it('lets non-401 errors pass through untouched', async () => {
    const instance = axios.create()
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)
    const config = {
      url: '/api/data',
      headers: {} as never,
    } as InternalAxiosRequestConfig
    const error = new AxiosError('Server Error', '500', config, null, {
      status: 500,
      data: null,
      headers: {} as never,
      config,
      statusText: 'Internal Server Error',
    })

    await expect(onRejected(error)).rejects.toBeDefined()
    expect(mockOnSessionExpired).not.toHaveBeenCalled()
  })

  it('lets 401 on /auth/login pass through without refreshing', async () => {
    const instance = axios.create()
    let refreshCalled = false
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCalled = true
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    const error = make401('/auth/login')
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(refreshCalled).toBe(false)
    expect(mockOnSessionExpired).not.toHaveBeenCalled()
  })

  it('lets 401 on /auth/2fa/confirm pass through without refreshing (wrong enrollment code)', async () => {
    const instance = axios.create()
    let refreshCalled = false
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCalled = true
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    const error = make401('/auth/2fa/confirm')
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(refreshCalled).toBe(false)
    expect(mockOnSessionExpired).not.toHaveBeenCalled()
  })

  it('lets 401 on /auth/2fa/verify pass through without refreshing (wrong TOTP code)', async () => {
    const instance = axios.create()
    let refreshCalled = false
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCalled = true
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    const error = make401('/auth/2fa/verify')
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(refreshCalled).toBe(false)
    expect(mockOnSessionExpired).not.toHaveBeenCalled()
  })

  it('lets 401 on /auth/refresh pass through without another refresh', async () => {
    const instance = axios.create()
    let refreshCallCount = 0
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCallCount++
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    const error = make401('/auth/refresh')
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(refreshCallCount).toBe(0)
  })

  it('does not retry requests that already have _retry=true', async () => {
    const instance = axios.create()
    let refreshCalled = false
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshCalled = true
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    const error = make401('/api/protected', true)
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(refreshCalled).toBe(false)
    expect(mockOnSessionExpired).not.toHaveBeenCalled()
  })

  it('triggers logout when refresh fails on 401', async () => {
    const instance = axios.create()
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        const refreshConfig = { url: '/auth/refresh', headers: {} as never } as InternalAxiosRequestConfig
        throw new AxiosError('Unauthorized', '401', refreshConfig, null, {
          status: 401,
          data: null,
          headers: {} as never,
          config: refreshConfig,
          statusText: 'Unauthorized',
        })
      }
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    const error = make401('/api/protected')
    await expect(onRejected(error)).rejects.toBeDefined()
    expect(mockOnSessionExpired).toHaveBeenCalledTimes(1)
  })

  it('queues concurrent 401s and retries all after successful refresh', async () => {
    const instance = axios.create()
    let refreshCallCount = 0
    const retried: string[] = []

    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshCallCount++
        return { data: {}, status: 204, statusText: 'No Content', headers: {}, config }
      }
      retried.push(config.url!)
      return { data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config }
    }

    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    const [, ,] = await Promise.all([
      onRejected(make401('/api/data1')),
      onRejected(make401('/api/data2')),
      onRejected(make401('/api/data3')),
    ])

    expect(refreshCallCount).toBe(1)
    expect(retried).toContain('/api/data1')
    expect(retried).toContain('/api/data2')
    expect(retried).toContain('/api/data3')
    expect(mockOnSessionExpired).not.toHaveBeenCalled()
  })

  it('triggers session expired only once when concurrent 401s arrive after failed refresh', async () => {
    const instance = axios.create()
    let refreshCallCount = 0

    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshCallCount++
        const refreshConfig = { url: '/auth/refresh', headers: {} as never } as InternalAxiosRequestConfig
        throw new AxiosError('Unauthorized', '401', refreshConfig, null, {
          status: 401,
          data: null,
          headers: {} as never,
          config: refreshConfig,
          statusText: 'Unauthorized',
        })
      }
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }

    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    await Promise.allSettled([
      onRejected(make401('/api/data1')),
      onRejected(make401('/api/data2')),
      onRejected(make401('/api/data3')),
    ])

    expect(refreshCallCount).toBe(1)
    expect(mockOnSessionExpired).toHaveBeenCalledTimes(1)
  })

  it('retries original request after successful refresh', async () => {
    const instance = axios.create()
    const retried: string[] = []
    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        return { data: {}, status: 204, statusText: 'No Content', headers: {}, config }
      }
      if (config.url === '/api/data') {
        retried.push(config.url)
        return { data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config }
      }
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }
    const { onRejected } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    const error = make401('/api/data')
    const result = await onRejected(error)
    expect((result as { data: { ok: boolean } }).data.ok).toBe(true)
    expect(retried).toContain('/api/data')
    expect(mockOnSessionExpired).not.toHaveBeenCalled()
  })

  it('resets sessionExpiredTriggered after successful login so next expiry triggers redirect', async () => {
    const instance = axios.create()
    let refreshCallCount = 0

    instance.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshCallCount++
        const refreshConfig = { url: '/auth/refresh', headers: {} as never } as InternalAxiosRequestConfig
        throw new AxiosError('Unauthorized', '401', refreshConfig, null, {
          status: 401, data: null, headers: {} as never, config: refreshConfig, statusText: 'Unauthorized',
        })
      }
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
    }

    const { onRejected, notifyLoginSuccess } = createRefreshInterceptorHandlers(instance, mockOnSessionExpired)

    // First 401 — refresh fails — sessionExpiredTriggered = true
    await expect(onRejected(make401('/api/data1'))).rejects.toBeDefined()
    expect(mockOnSessionExpired).toHaveBeenCalledTimes(1)

    // Simulate re-login via callback
    notifyLoginSuccess()

    // Second 401 — sessionExpiredTriggered must have been reset — must trigger again
    await expect(onRejected(make401('/api/data2'))).rejects.toBeDefined()
    expect(mockOnSessionExpired).toHaveBeenCalledTimes(2)
    // Each 401 triggers exactly one refresh attempt
    expect(refreshCallCount).toBe(2)
  })
})