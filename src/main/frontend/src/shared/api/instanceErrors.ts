import { isAxiosError } from 'axios'
import { NetworkError, parseRetryAfter, RateLimitError, ServerError } from '@/shared/lib'

/** The server refused some settings; `errors` maps a setting's code to a problem code (see common:setting_problem). */
export class SettingsInvalidError extends Error {
  readonly errors: Record<string, string>
  constructor(errors: Record<string, string>) {
    super('Some settings are not valid')
    this.name = 'SettingsInvalidError'
    this.errors = errors
  }
}

/** The setting is set by a variable of the environment and cannot be changed from a page. */
export class SettingLockedError extends Error {
  readonly setting: string | null
  constructor(setting: string | null) {
    super('Setting locked by the environment')
    this.name = 'SettingLockedError'
    this.setting = setting
  }
}

/**
 * The test mail did not leave. `reason` says why (see common:mail_failure); `serverReply` is what the
 * mail server answered, when it answered as a mail server does — null otherwise, never another
 * service's words.
 */
export class MailTestFailedError extends Error {
  readonly reason: string
  readonly serverReply: string | null
  constructor(reason: string, serverReply: string | null) {
    super('Test mail failed')
    this.name = 'MailTestFailedError'
    this.reason = reason
    this.serverReply = serverReply
  }
}

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
