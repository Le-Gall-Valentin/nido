import type { AxiosInstance, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { setLoginSuccessCallback, triggerSessionExpired } from '@/shared/lib'

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
 * Routes that authenticate the request by what it carries — a code, or the challenge cookie of a sign-in in
 * progress — rather than by the access token. Their 401 says "that code is wrong" (or "start the sign-in
 * again") while the session is fine, so refreshing rotates both tokens for nothing and replaying the request
 * spends one of the attempts the server allows. The method is part of the rule: /auth/2fa/{method}/setup,
 * /auth/2fa/mail/disable-code and GET /auth/2fa carry no code, so a 401 from those is what refreshing is for.
 */
const CODE_BEARING_ROUTES: ReadonlyArray<{ method: string; path: RegExp }> = [
  { method: 'post', path: /\/auth\/2fa\/verify$/ },
  { method: 'post', path: /\/auth\/2fa\/challenge\/mail$/ },
  { method: 'post', path: /\/auth\/2fa\/(app|mail)\/confirm$/ },
  { method: 'delete', path: /\/auth\/2fa\/(app|mail)$/ },
]

function answersSomethingOtherThanTheSession(request: InternalAxiosRequestConfig): boolean {
  const path = request.url?.split('?')[0]
  if (!path) return false
  const method = request.method?.toLowerCase()
  return SESSION_ROUTES.some((route) => path.endsWith(route))
    || CODE_BEARING_ROUTES.some((route) => route.method === method && route.path.test(path))
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

    const original = error.config

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
      throw refreshError
    } finally {
      isRefreshing = false
    }
  }

  return { onFulfilled, onRejected, notifyLoginSuccess }
}

export function attachRefreshInterceptor(client: AxiosInstance): void {
  const { onFulfilled, onRejected, notifyLoginSuccess } = createRefreshInterceptorHandlers(client, triggerSessionExpired)
  client.interceptors.response.use(onFulfilled, onRejected)
  // Registered rather than returned: this used to travel out through the client's exports, so the
  // auth store imported an axios module to say "somebody signed in".
  setLoginSuccessCallback(notifyLoginSuccess)
}
