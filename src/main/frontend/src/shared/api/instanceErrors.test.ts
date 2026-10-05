import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it } from 'vitest'
import { MailTestFailedError, NetworkError, RateLimitError, ServerError, SettingLockedError, SettingsInvalidError } from '@/shared/lib'
import { toInstanceError } from './instanceErrors'

function answer(status: number, data: object = {}, headers: Record<string, string> = {}) {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR', config, null, { status, statusText: '', data, headers, config })
}

describe('toInstanceError', () => {
  it('reads the problems setting by setting', () => {
    const error = toInstanceError(answer(400, { error_code: 'SETTINGS_INVALID', errors: { 'mail.port': 'out_of_range' } }))
    expect(error).toBeInstanceOf(SettingsInvalidError)
    expect((error as SettingsInvalidError).errors).toEqual({ 'mail.port': 'out_of_range' })
  })

  it('says which setting the environment holds', () => {
    const error = toInstanceError(answer(409, { error_code: 'SETTING_LOCKED_BY_ENVIRONMENT', setting: 'api.swagger' }))
    expect((error as SettingLockedError).setting).toBe('api.swagger')
  })

  it('keeps why the test mail failed and what the mail server answered', () => {
    const error = toInstanceError(answer(422, {
      error_code: 'MAIL_TEST_FAILED', reason: 'authentication_failed',
      server_reply: '535 Authentication failed', detail: '535 Authentication failed',
    }))
    expect((error as MailTestFailedError).reason).toBe('authentication_failed')
    expect((error as MailTestFailedError).serverReply).toBe('535 Authentication failed')
  })

  it('has no reply to show when the server did not answer as a mail server does', () => {
    const error = toInstanceError(answer(422, { error_code: 'MAIL_TEST_FAILED', reason: 'connection_refused', detail: 'The test mail could not be sent.' }))
    expect((error as MailTestFailedError).reason).toBe('connection_refused')
    expect((error as MailTestFailedError).serverReply).toBeNull()
  })

  it('lets the caller name its own errors first', () => {
    const own = new Error('own')
    expect(toInstanceError(answer(404), (status) => (status === 404 ? own : null))).toBe(own)
  })

  it('falls back on the usual errors', () => {
    expect(toInstanceError(answer(429, {}, { 'retry-after': '30' }))).toBeInstanceOf(RateLimitError)
    expect(toInstanceError(answer(500))).toBeInstanceOf(ServerError)
    expect(toInstanceError(new Error('offline'))).toBeInstanceOf(NetworkError)
  })
})
