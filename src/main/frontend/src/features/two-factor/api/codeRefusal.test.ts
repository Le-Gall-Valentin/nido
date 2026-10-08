// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { codeRefusalOf } from './codeRefusal'
import { ResendTooSoonError, SendLimitError } from '../model/errors'

describe('codeRefusalOf', () => {
  it('reads the wait from the body first, then from Retry-After, then a default', () => {
    expect(codeRefusalOf({ data: { error_code: 'resend_too_soon', retryAfterSeconds: 42 }, headers: { 'retry-after': '50' } }))
      .toEqual(new ResendTooSoonError(42))
    expect(codeRefusalOf({ data: { error_code: 'send_limit_reached' }, headers: { 'retry-after': '600' } }))
      .toEqual(new SendLimitError(600))
    expect(codeRefusalOf({ data: { error_code: 'resend_too_soon' }, headers: {} })).toEqual(new ResendTooSoonError(60))
    expect(codeRefusalOf({ data: { error_code: 'send_limit_reached' }, headers: {} })).toEqual(new SendLimitError(900))
  })

  it('anything else is not a refusal of a code', () => {
    expect(codeRefusalOf({ data: { error_code: 'code_spent' }, headers: {} })).toBeNull()
    expect(codeRefusalOf({ data: undefined, headers: {} })).toBeNull()
  })
})
