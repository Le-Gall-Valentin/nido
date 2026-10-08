import { describe, expect, it } from 'vitest'
import { commonErrorMessage } from './messages'
import { SendLimitError } from './errors'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

describe('commonErrorMessage', () => {
  it('says when the next code can leave, in minutes', () => {
    expect(commonErrorMessage(new SendLimitError(540))).toEqual({ key: 'twoFactor:error.send_limit', values: { minutes: 9 } })
  })

  it('says how long the route limit holds, when it says', () => {
    // Reached in a few tries on PATCH /me (5 a minute): "an error occurred" would have the person try again at once.
    expect(commonErrorMessage(new RateLimitError(45))).toEqual({ key: 'twoFactor:error.rate_limit_timed', values: { seconds: 45 } })
    expect(commonErrorMessage(new RateLimitError())).toEqual({ key: 'twoFactor:error.rate_limit' })
  })

  it('tells a lost network from a server error, and the unknown from both', () => {
    expect(commonErrorMessage(new NetworkError())).toEqual({ key: 'twoFactor:error.network' })
    expect(commonErrorMessage(new ServerError())).toEqual({ key: 'twoFactor:error.server' })
    expect(commonErrorMessage(new Error('?'))).toEqual({ key: 'twoFactor:error.server' })
  })
})
