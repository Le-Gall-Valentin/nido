import type { AxiosInstance, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { triggerSessionExpired } from '@/shared/lib'

interface QueueEntry {
  resolve: (value: unknown) => void
  reject: (reason: unknown) => void
  config: InternalAxiosRequestConfig
}

/**
 * Routes whose 401 is about the session itself, where refreshing would either loop or say nothing:
 * signing in, refreshing, signing out.
 */
const SESSION_ROUTES = ['/auth/login', '/auth/refresh', '/auth/logout'] as const

/**
 * Routes that authenticate the request by what it carries — a TOTP code, or the challenge cookie
 * from a login in progress — rather than by the access token. Their 401 says "that code is wrong"
 * while the session is perfectly valid, so refreshing rotates both tokens for nothing and replaying
 * the request spends a second of the attempts the server allows on it. The method is part of the
 * rule: the disable route is the DELETE on /auth/2fa, while /auth/2fa/setup and /auth/2fa/status sit
 * under the same prefix and carry no code, so a 401 from those two is what refreshing is for.
 */
const CODE_BEARING_ROUTES = [
  { method: 'post', path: '/auth/2fa/verify' },
  { method: 'post', path: '/auth/2fa/confirm' },
  { method: 'delete', path: '/auth/2fa' },
] as const

function answersSomethingOtherThanTheSession(request: InternalAxiosRequestConfig): boolean {
  const path = request.url?.split('?')[0]
  if (!path) return false
  const method = request.method?.toLowerCase()
  return SESSION_ROUTES.some((route) => path.endsWith(route))
    || CODE_BEARING_ROUTES.some((route) => path.endsWith(route.path) && route.method === method)
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

    if (answersSomethingOtherThanTheSession(original)) {
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
