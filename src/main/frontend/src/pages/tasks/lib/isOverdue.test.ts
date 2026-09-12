import { describe, expect, it } from 'vitest'
import { isOverdue } from './isOverdue'

describe('isOverdue', () => {
  it('a task due today is not late', () => {
    expect(isOverdue('2026-09-12', '2026-09-12')).toBe(false)
  })

  it('a task due yesterday is late', () => {
    expect(isOverdue('2026-09-11', '2026-09-12')).toBe(true)
  })

  it('a task with no due date is never late', () => {
    expect(isOverdue(null, '2026-09-12')).toBe(false)
  })

  it('lateness is decided on the household calendar, not the viewer one', () => {
    // The bug this fixes, and the one a member abroad actually sees: a task due on the 12th, read
    // from Toronto at half past eight in the evening. The viewer's own date is still the 12th, so
    // the task is not late — but computing it from a UTC instant reported tomorrow and the card
    // was struck through while its day was still going.
    expect(isOverdue('2026-09-12', '2026-09-12')).toBe(false)
    // And the mirror: the household in Paris has already turned the page, so it is late there.
    expect(isOverdue('2026-09-12', '2026-09-13')).toBe(true)
  })
})
