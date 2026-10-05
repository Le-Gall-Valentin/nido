import { renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MailTestFailedError } from '../model/errors'
import { useSettingWording } from './useSettingWording'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

describe('useSettingWording', () => {
  it('words each problem the server knows, and an unknown one as such', () => {
    const { result } = renderHook(() => useSettingWording())

    expect(result.current.problem('required')).toBe('problem.required')
    expect(result.current.problem('password_required_for_new_server')).toBe('problem.password_required_for_new_server')
    expect(result.current.problem('a_code_of_a_later_version')).toBe('problem.unknown')
  })

  it('words why a test mail failed, with what the server answered when it answered', () => {
    const { result } = renderHook(() => useSettingWording())

    expect(result.current.mailFailure(new MailTestFailedError('authentication_failed', '535 Bad credentials')))
      .toBe('mail_failure.authentication_failed — 535 Bad credentials')
    expect(result.current.mailFailure(new MailTestFailedError('timeout', null))).toBe('mail_failure.timeout')
    expect(result.current.mailFailure(new MailTestFailedError('a_reason_of_a_later_version', null))).toBe('mail_failure.failed')
  })
})
