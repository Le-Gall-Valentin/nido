import { describe, it, expect } from 'vitest'
import { filterMine, isMine } from './filterMine'

describe('filterMine', () => {
  const mine = { id: 'a', assigneeIds: ['u-me'] }
  const nobodys = { id: 'b', assigneeIds: [] }
  const shared = { id: 'c', assigneeIds: ['u-cam', 'u-me'] }
  const theirs = { id: 'd', assigneeIds: ['u-cam'] }

  it('keeps what is assigned to me and what is assigned to nobody', () => {
    expect(filterMine([mine, nobodys, shared, theirs], 'u-me').map((t) => t.id)).toEqual(['a', 'b', 'c'])
  })

  it('keeps only the unassigned ones while the caller is unknown', () => {
    expect(isMine(mine, null)).toBe(false)
    expect(isMine(nobodys, null)).toBe(true)
  })
})
