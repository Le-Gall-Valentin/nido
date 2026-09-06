import { describe, expect, it } from 'vitest'
import { resolveContributorsOrError } from './resolveContributorsOrError'

const t = (key: string) => key

describe('resolveContributorsOrError', () => {
  it('requires a payer when contributors can be picked', () => {
    const result = resolveContributorsOrError(50, true, '', ['alice'], false, {}, t)

    expect(result).toEqual({ contributors: null, error: 'form.payer_required' })
  })

  it('does not require a payer when contributors cannot be picked', () => {
    const result = resolveContributorsOrError(50, false, '', [], false, {}, t)

    expect(result).toEqual({ contributors: [], error: null })
  })

  it('resolves an equal split when shares are not customized', () => {
    const result = resolveContributorsOrError(50, true, 'alice', ['alice', 'bob'], false, {}, t)

    expect(result).toEqual({ contributors: [{ memberId: 'alice', shareAmount: null }, { memberId: 'bob', shareAmount: null }], error: null })
  })

  it('rejects custom shares that do not sum to the amount', () => {
    const result = resolveContributorsOrError(50, true, 'alice', ['alice', 'bob'], true, { alice: 10, bob: 10 }, t)

    expect(result).toEqual({ contributors: null, error: 'form.shares_invalid' })
  })

  it('resolves valid custom shares', () => {
    const result = resolveContributorsOrError(50, true, 'alice', ['alice', 'bob'], true, { alice: 30, bob: 20 }, t)

    expect(result).toEqual({ contributors: [{ memberId: 'alice', shareAmount: 30 }, { memberId: 'bob', shareAmount: 20 }], error: null })
  })
})
