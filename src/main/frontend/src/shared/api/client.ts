import axios from 'axios'
import { attachRefreshInterceptor } from './refreshInterceptor'

// CSRF: no token header needed. Two complementary protections apply:
// 1. HttpOnly cookies with SameSite=Strict block cross-origin forged requests.
// 2. Content-Type: application/json triggers a CORS preflight on cross-origin requests,
//    which browsers block unless the server explicitly allows the origin — providing
//    an additional layer even when SameSite is not honoured (e.g. older browsers).
// Spring Security CSRF protection is explicitly disabled on the backend for this reason.
const API_TIMEOUT_MS = 15_000

export const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: API_TIMEOUT_MS,
})

export const notifyLoginSuccess = attachRefreshInterceptor(client)