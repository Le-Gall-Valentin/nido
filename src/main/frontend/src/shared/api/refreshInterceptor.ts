import type { AxiosInstance, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { triggerSessionExpired } from '@/shared/lib'

interface QueueEntry {
  resolve: (value: unknown) => void
  reject: (reason: unknown) => void
  config: InternalAxiosRequestConfig
}

export function createRefreshInterceptorHandlers(
  client: AxiosInstance,
  onSessionExpired: () => void
): {
  onFulfilled: (response: AxiosResponse) => AxiosResponse
  onRejected: (error: AxiosError) => Promise<unknown>
  notifyLoginSuccess: () => void
} {
  let isRefreshing = false
  let failedQueue: QueueEntry[] = []
  let sessionExpiredTriggered = false

  function notifyLoginSuccess(): void {
    sessionExpiredTriggered = false
  }

  function flushQueue(error: unknown): void {
    failedQueue.forEach(({ resolve, reject, config }) => {
      if (error) reject(error)
      else {
        config._retry = true
        resolve(client(config))
      }
    })
    failedQueue = []
  }

  function onFulfilled(response: AxiosResponse): AxiosResponse {
    return response
  }

  async function onRejected(error: AxiosError): Promise<unknown> {
    if (!error.config) {
      return Promise.reject(error)
    }

    const original = error.config as InternalAxiosRequestConfig

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    const requestPath = original.url?.split('?')[0]
    if (
      requestPath?.endsWith('/auth/login') ||
      requestPath?.endsWith('/auth/refresh') ||
      requestPath?.endsWith('/auth/logout') ||
      // TOTP endpoints authenticated via challenge cookie or TOTP code validation, not JWT.
      // A 401 from these paths means wrong code — must NOT trigger a refresh or session expiry.
      requestPath?.endsWith('/auth/2fa/verify') ||
      requestPath?.endsWith('/auth/2fa/confirm')
    ) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject, config: original })
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      await client.post('/auth/refresh')
      sessionExpiredTriggered = false
      flushQueue(null)
      return client(original)
    } catch (refreshError) {
      flushQueue(refreshError)
      if (!sessionExpiredTriggered) {
        sessionExpiredTriggered = true
        onSessionExpired()
      }
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }

  return { onFulfilled, onRejected, notifyLoginSuccess }
}

export function attachRefreshInterceptor(client: AxiosInstance): () => void {
  const { onFulfilled, onRejected, notifyLoginSuccess } = createRefreshInterceptorHandlers(client, triggerSessionExpired)
  client.interceptors.response.use(onFulfilled, onRejected)
  return notifyLoginSuccess
}
