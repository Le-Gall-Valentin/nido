import { describe, it, expect } from 'vitest'
import { pageAfterRemoval } from './pageAfterRemoval'

describe('pageAfterRemoval', () => {
  it('steps back when the last row of a page beyond the first is removed', () => {
    expect(pageAfterRemoval(2, 1, false)).toBe(1)
  })

  it('stays on the same page when other rows remain', () => {
    expect(pageAfterRemoval(2, 3, false)).toBe(2)
  })

  it('never steps back below the first page', () => {
    expect(pageAfterRemoval(0, 1, false)).toBe(0)
  })

  it('does not act on placeholder (mid-transition) data', () => {
    expect(pageAfterRemoval(2, 1, true)).toBe(2)
  })
})
