import { describe, expect, it } from 'vitest'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import { KeyNotSavedError, SetupAlreadyDoneError, SetupCodeInvalidError } from '../model/errors'
import { describeSetupError } from './describeSetupError'

describe('describeSetupError', () => {
  it('words each error of the setup routes', () => {
    expect(describeSetupError(new SetupCodeInvalidError())).toBe('errors.code_invalid')
    expect(describeSetupError(new SetupAlreadyDoneError())).toBe('errors.already_done')
    expect(describeSetupError(new KeyNotSavedError())).toBe('errors.key_not_saved')
    expect(describeSetupError(new RateLimitError(null))).toBe('errors.too_many')
    expect(describeSetupError(new NetworkError())).toBe('errors.network')
  })

  it('falls back on a server error for anything else', () => {
    expect(describeSetupError(new ServerError())).toBe('errors.server')
    expect(describeSetupError(new Error('boom'))).toBe('errors.server')
  })
})
