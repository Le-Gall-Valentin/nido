import { isAxiosError } from 'axios'
import { MailTestFailedError, NetworkError, parseRetryAfter, RateLimitError, ServerError, SettingLockedError, SettingsInvalidError } from '@/shared/lib'

interface ProblemBody {
  error_code?: string
  errors?: Record<string, string>
  setting?: string
  reason?: string
  server_reply?: string
}

/**
 * The errors of the setup and settings routes. `specific` names a page's own errors first — a wrong
 * setup code, a setup already done — before the ones both pages share.
 */
export function toInstanceError(
  error: unknown,
  specific: (status: number, code: string | undefined) => Error | null = () => null,
): Error {
  if (isAxiosError<ProblemBody>(error) && error.response) {
    const { status, data, headers } = error.response
    const code = data?.error_code
    const known = specific(status, code)
    if (known) return known
    if (code === 'SETTINGS_INVALID') return new SettingsInvalidError(data?.errors ?? {})
    if (code === 'SETTING_LOCKED_BY_ENVIRONMENT') return new SettingLockedError(data?.setting ?? null)
    if (code === 'MAIL_TEST_FAILED') return new MailTestFailedError(data?.reason ?? 'failed', data?.server_reply ?? null)
    if (status === 429) return new RateLimitError(parseRetryAfter(headers))
    return new ServerError()
  }
  return new NetworkError()
}
