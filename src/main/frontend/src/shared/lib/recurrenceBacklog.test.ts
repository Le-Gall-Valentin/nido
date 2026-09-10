import { describe, expect, it } from 'vitest'
import { MAX_PAST_OCCURRENCES, pastOccurrenceCount } from './recurrenceBacklog'

const TODAY = '2026-09-09'

describe('pastOccurrenceCount', () => {
  it('counts the anchor itself as the first occurrence', () => {
    expect(pastOccurrenceCount(TODAY, 'DAILY', 1, null, TODAY)).toBe(1)
  })

  it('counts nothing for a series anchored in the future', () => {
    expect(pastOccurrenceCount('2027-01-01', 'DAILY', 1, null, TODAY)).toBe(0)
  })

  it('counts one occurrence per day for a daily series', () => {
    expect(pastOccurrenceCount('2026-09-01', 'DAILY', 1, null, TODAY)).toBe(9)
  })

  it('honours a multi-day step', () => {
    // 1, 4, 7 September — the 10th falls after today.
    expect(pastOccurrenceCount('2026-09-01', 'DAILY', 3, null, TODAY)).toBe(3)
  })

  it('counts weekly occurrences', () => {
    expect(pastOccurrenceCount('2026-08-19', 'WEEKLY', 1, null, TODAY)).toBe(4)
  })

  it('counts monthly occurrences from the start of the year', () => {
    expect(pastOccurrenceCount('2026-01-01', 'MONTHLY', 1, null, TODAY)).toBe(9)
  })

  it('counts yearly occurrences', () => {
    expect(pastOccurrenceCount('2020-09-09', 'YEARLY', 1, null, TODAY)).toBe(7)
  })

  it('stops at the end date when one is set', () => {
    expect(pastOccurrenceCount('2026-09-01', 'DAILY', 1, '2026-09-04', TODAY)).toBe(4)
  })

  it('handles a month-end anchor without drifting', () => {
    // 31 Jan, 28 Feb, 31 Mar — the clamped February must not pull March back to the 28th.
    expect(pastOccurrenceCount('2026-01-31', 'MONTHLY', 1, null, '2026-03-30')).toBe(2)
    expect(pastOccurrenceCount('2026-01-31', 'MONTHLY', 1, null, '2026-03-31')).toBe(3)
  })

  it('stays cheap for an anchor far enough back to freeze a naive loop', () => {
    const started = performance.now()
    const count = pastOccurrenceCount('0001-01-01', 'DAILY', 1, null, TODAY)
    const elapsed = performance.now() - started

    expect(count).toBeGreaterThan(MAX_PAST_OCCURRENCES)
    expect(elapsed).toBeLessThan(50)
  })

  it('does not mistake a two-digit year for a twentieth-century one', () => {
    // Date.UTC(50, ...) means 1950; a series anchored in year 0050 must not be measured
    // from there, or its backlog would come out roughly two millennia short.
    expect(pastOccurrenceCount('0050-01-01', 'YEARLY', 1, null, TODAY)).toBe(1977)
  })

  it('returns nothing for a half-typed or unusable form', () => {
    expect(pastOccurrenceCount('', 'DAILY', 1, null, TODAY)).toBe(0)
    expect(pastOccurrenceCount('2026-09', 'DAILY', 1, null, TODAY)).toBe(0)
    expect(pastOccurrenceCount('2026-09-01', 'DAILY', 0, null, TODAY)).toBe(0)
  })
})
