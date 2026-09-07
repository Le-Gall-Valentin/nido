import { describe, it, expect } from 'vitest'
import { leadTimeExceedsInterval } from './leadTimeExceedsInterval'

describe('leadTimeExceedsInterval', () => {
  it('is false when the lead time is shorter than the interval', () => {
    expect(leadTimeExceedsInterval('2026-01-07', 'MONTHLY', 1, 'WEEKLY', 1)).toBe(false)
  })

  it('is false when the lead time exactly equals the interval', () => {
    expect(leadTimeExceedsInterval('2026-01-07', 'WEEKLY', 1, 'DAILY', 7)).toBe(false)
  })

  it('is true when the lead time is longer than the interval', () => {
    expect(leadTimeExceedsInterval('2026-01-07', 'WEEKLY', 1, 'DAILY', 8)).toBe(true)
  })

  it('is false for a zero lead time regardless of unit', () => {
    expect(leadTimeExceedsInterval('2026-01-07', 'DAILY', 1, 'YEARLY', 0)).toBe(false)
  })

  it('clamps month-end anchors the same way on both sides of the comparison', () => {
    // anchor 2026-01-31: +1 month -> 2026-02-28 (main), +1 month lead -> 2026-02-28 too. Equal, not exceeding.
    expect(leadTimeExceedsInterval('2026-01-31', 'MONTHLY', 1, 'MONTHLY', 1)).toBe(false)
  })

  it('clamps a yearly anchor on a leap day to Feb 28, mirroring the backend', () => {
    // anchor 2028-02-29 (leap year), +1 year should clamp to 2029-02-28, not roll over
    // to 2029-03-01 the way an unclamped setUTCFullYear would. A 366-day lead lands
    // exactly on 2029-03-01, which is one day past the correctly clamped main date.
    expect(leadTimeExceedsInterval('2028-02-29', 'YEARLY', 1, 'DAILY', 366)).toBe(true)
  })
})
